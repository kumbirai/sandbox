package com.kumbirai.sandbox;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class RecursiveFileDelete
{
	private static StringBuilder SB;

	public static void main(String[] args)
	{
		SB = new StringBuilder();
		Path rootPath = FileSystems.getDefault()
				.getPath("G:\\");
		try (Stream<Path> walk = Files.walk(rootPath))
		{
			Pattern pattern = Pattern.compile("\\([\\d]\\)(\\.[\\w]{3,4})$");
			walk.map(Path::toFile)
					.filter(File::isFile)
					.filter(file -> pattern.matcher(file.toString())
							.matches())
					//.peek(RecursiveFileDelete::fileDetails)
					.forEach(RecursiveFileDelete::fileDetails);
			//.forEach(File::delete);
			Path report = Paths.get("RecursiveFileDeleteOutput.txt");
			Files.write(report,
					SB.toString()
							.getBytes());
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	public static void fileDetails(File file)
	{
		BasicFileAttributeView basicView = Files.getFileAttributeView(file.toPath(),
				BasicFileAttributeView.class);
		try
		{
			BasicFileAttributes basicFileAttributes = basicView.readAttributes();
			String output = String.format("%s%slastAccessTime: %s, lastModifiedTime: %s, creationTime: %s%s",
					file.getPath(),
					System.lineSeparator(),
					basicFileAttributes.lastAccessTime(),
					basicFileAttributes.lastModifiedTime(),
					basicFileAttributes.creationTime(),
					System.lineSeparator());
			System.out.println(output);
			SB.append(output);
			//ImageIO.read(file);
		}
		catch (IOException e)
		{
			System.err.println(e);
		}
	}
}
