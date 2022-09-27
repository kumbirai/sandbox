package com.kumbirai.sandbox;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class DuplicateFiles
{
	public static void main(String[] args)
	{
		try
		{
			findDuplicateFiles("G:\\My Drive\\Chihwa\\Canon",
					1000);
		}
		catch (IOException | NoSuchAlgorithmException e)
		{
			System.err.println(e.fillInStackTrace());
		}
	}

	private static void findDuplicateFiles(String directory, long minimumSize) throws IOException, NoSuchAlgorithmException
	{
		System.out.println("Directory: '" + directory + "', minimum size: " + minimumSize + " bytes.");
		Path path = FileSystems.getDefault()
				.getPath(directory);
		FileVisitor visitor = new FileVisitor(path,
				minimumSize);
		System.out.println("Processing files...");
		Files.walkFileTree(path,
				visitor);
		System.out.println("<><><><><><><><><><>");
		System.out.println("The following sets of files have the same size and checksum:");
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<FileKey, Map<Object, List<String>>> e : visitor.fileMap_.entrySet())
		{
			Map<Object, List<String>> map = e.getValue();
			if (!containsDuplicates(map))
			{
				continue;
			}
			List<List<String>> fileSets = new ArrayList<>(map.values());
			for (List<String> files : fileSets)
			{
				Collections.sort(files);
			}
			Collections.sort(fileSets,
					new StringListComparator());
			printResults(e,
					fileSets,
					sb);
		}
		Path report = Paths.get("DuplicateFilesOutput.txt");
		Files.write(report,
				sb.toString()
						.getBytes());
	}

	private static void printResults(Map.Entry<FileKey, Map<Object, List<String>>> e, List<List<String>> fileSets, StringBuilder sb)
	{
		FileKey key = e.getKey();
		System.out.println();
		String sizeText = String.format("Size: %s bytes [%s]",
				key.size_,
				key.hash_);
		System.out.println(sizeText);
		sb.append(sizeText)
				.append(System.lineSeparator());
		for (List<String> files : fileSets)
		{
			for (int i = 0, n = files.size(); i < n; ++i)
			{
				if (i > 0)
				{
					System.out.print(" = ");
				}
				System.out.print(files.get(i));
			}
			System.out.println();
		}
		fileSets.forEach(files -> files.forEach(file ->
		{
			System.out.println(file);
			sb.append(file)
					.append(System.lineSeparator());
		}));
	}

	private static boolean containsDuplicates(Map<Object, List<String>> map)
	{
		if (map.size() > 1)
		{
			return true;
		}
		for (List<String> files : map.values())
		{
			if (files.size() > 1)
			{
				return true;
			}
		}
		return false;
	}

	private static int hashCompare(byte[] a, byte[] b)
	{
		int len1 = a.length, len2 = b.length;
		for (int i = 0; i < len1 && i < len2; ++i)
		{
			int c = Byte.compare(a[i],
					b[i]);
			if (c != 0)
			{
				return c;
			}
		}
		return Integer.compare(len1,
				len2);
	}

	private static class StringListComparator implements Comparator<List<String>>
	{
		public int compare(List<String> a, List<String> b)
		{
			int len1 = a.size(), len2 = b.size();
			for (int i = 0; i < len1 && i < len2; ++i)
			{
				int c = a.get(i)
						.compareTo(b.get(i));
				if (c != 0)
				{
					return c;
				}
			}
			return Integer.compare(len1,
					len2);
		}
	}

	private static class FileVisitor extends SimpleFileVisitor<Path>
	{
		private final MessageDigest digest_;
		private final Path directory_;
		private final long minimumSize_;
		private final Map<FileKey, Map<Object, List<String>>> fileMap_ = new TreeMap<>();

		private FileVisitor(Path directory, long minimumSize) throws NoSuchAlgorithmException
		{
			directory_ = directory;
			minimumSize_ = minimumSize;
			digest_ = MessageDigest.getInstance("MD5");
		}

		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
		{
			System.out.println(file);
			if (attrs.size() >= minimumSize_)
			{
				FileKey key = new FileKey(file,
						attrs,
						getMD5Sum(file));
				Map<Object, List<String>> map = fileMap_.get(key);
				if (map == null)
				{
					fileMap_.put(key,
							map = new HashMap<>());
				}
				List<String> files = map.get(attrs.fileKey());
				if (files == null)
				{
					map.put(attrs.fileKey(),
							files = new ArrayList<>());
				}
				//Path relative = directory_.relativize(file);
				files.add(file.toString());
			}
			return FileVisitResult.CONTINUE;
		}

		private byte[] getMD5Sum(Path file) throws IOException
		{
			digest_.reset();
			try (InputStream in = new FileInputStream(file.toString()))
			{
				byte[] buffer = new byte[8192];
				int bytes;
				while ((bytes = in.read(buffer)) != -1)
				{
					digest_.update(buffer,
							0,
							bytes);
				}
			}
			return digest_.digest();
		}
	}

	private static class FileKey implements Comparable<FileKey>
	{
		private final byte[] hash_;
		private final long size_;

		private FileKey(Path file, BasicFileAttributes attrs, byte[] hash) throws IOException
		{
			size_ = attrs.size();
			hash_ = hash;
		}

		public int compareTo(FileKey other)
		{
			int c = Long.compare(other.size_,
					size_);
			if (c == 0)
			{
				c = hashCompare(hash_,
						other.hash_);
			}
			return c;
		}
	}
}
