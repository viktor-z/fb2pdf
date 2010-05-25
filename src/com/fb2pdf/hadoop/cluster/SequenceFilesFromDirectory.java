package com.fb2pdf.hadoop.cluster;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.SequenceFile.CompressionType;
import org.apache.hadoop.io.compress.CompressionCodec;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

public final class SequenceFilesFromDirectory extends Configured implements
		Tool {

	private transient static Logger LOG = LoggerFactory
			.getLogger(SequenceFilesFromDirectory.class);
	Configuration conf = new Configuration();

	public static class ChunkedWriter implements Closeable {
		private final int maxChunkSizeInBytes;
		private final String outputDir;
		private SequenceFile.Writer writer;
		private int currentChunkID;
		private int currentChunkSize;
		private final Configuration conf = new Configuration();
		private final FileSystem fs;

		public ChunkedWriter(int chunkSizeInMB, String outputDir)
				throws IOException {
			if (chunkSizeInMB < 64) {
				chunkSizeInMB = 64;
			} else if (chunkSizeInMB > 1984) {
				chunkSizeInMB = 1984;
			}
			maxChunkSizeInBytes = chunkSizeInMB * 1024 * 1024;
			this.outputDir = outputDir;
			fs = FileSystem.get(conf);
			currentChunkID = 0;
			conf.setClass("mapred.output.compression.codec", GzipCodec.class, CompressionCodec.class);
			CompressionCodec codec = new GzipCodec();
			writer = SequenceFile.createWriter(fs, conf, getPath(currentChunkID), Text.class, Text.class, CompressionType.RECORD, codec);
		}

		private Path getPath(int chunkID) {
			return new Path(outputDir + "/chunk-" + chunkID);
		}

		public void write(String key, String value) throws IOException {
			if (currentChunkSize > maxChunkSizeInBytes) {
				writer.close();
				writer = new SequenceFile.Writer(fs, conf,
						getPath(currentChunkID++), Text.class, Text.class);
				currentChunkSize = 0;

			}

			Text keyT = new Text(key);
			Text valueT = new Text(value);
			currentChunkSize += keyT.getBytes().length
					+ valueT.getBytes().length; // Overhead
			writer.append(keyT, valueT);
		}

		@Override
		public void close() throws IOException {
			writer.close();
		}
	}

	public class PrefixAdditionFilter implements PathFilter {
		private final String prefix;
		private final ChunkedWriter writer;
		private final Charset charset;
		private final FileSystem fs;

		public PrefixAdditionFilter(FileSystem fs, String prefix,
				ChunkedWriter writer, Charset charset) {
			this.fs = fs;
			this.prefix = prefix;
			this.writer = writer;
			this.charset = charset;
		}

		@Override
		public boolean accept(Path current) {
			try {
				if (fs.getFileStatus(current).isDir()) {
					try {
						fs.listStatus(current, new PrefixAdditionFilter(fs,
								prefix + File.separator + current.getName(),
								writer, charset));
					} catch (IOException e) {
						LOG.error(e.getMessage());
					}
				} else {
					try {
						StringBuilder file = new StringBuilder();
						BufferedReader fileReader = new BufferedReader(
								new InputStreamReader(fs.open(current)));
						String line = null;
						while ((line = fileReader.readLine()) != null) {
							String[] tokenized = line.split("\t");
							if (tokenized.length == 2) {
								try {
									long amountOfWords = Math.abs(Long
											.parseLong(tokenized[1]));
									for (int i = 0; i < amountOfWords; i++) {
										file.append(tokenized[0]).append(" ");
									}
								} catch (NumberFormatException e) {
									// do nothing
								}
							}
						}
						writer.write(current.getParent().getName(), file.toString());

					} catch (FileNotFoundException e) {
						// Skip file.
					} catch (IOException e) {
						// TODO: report exceptions and continue;
						throw new IllegalStateException(e);
					}
				}
			} catch (IOException e) {
				LOG.error(e.getMessage());
			}
			return false;
		}

	}

	private static ChunkedWriter createNewChunkedWriter(int chunkSizeInMB,
			String outputDir) throws IOException {
		return new ChunkedWriter(chunkSizeInMB, outputDir);
	}

	public void createSequenceFiles(Path parentDir, String outputDir,
			String prefix, int chunkSizeInMB, Charset charset)
			throws IOException {
		ChunkedWriter writer = createNewChunkedWriter(chunkSizeInMB, outputDir);
		FileSystem fs = FileSystem.get(conf);
		fs.listStatus(parentDir, new PrefixAdditionFilter(fs, prefix, writer,
				charset));
		writer.close();
	}

	@Override
	public void setConf(Configuration conf) {
		this.conf = conf;
	}

	@Override
	public int run(String[] args) throws Exception {
		Charset charset = Charset.forName("UTF-8");
		SequenceFilesFromDirectory dir = new SequenceFilesFromDirectory();
		dir.createSequenceFiles(new Path(args[0]), args[1], args[2], 64,
				charset);
		return 0;
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 3) {
			System.err
					.println("Usage SequenceFilesFromDirectory <src> <dst> <prefix>");
			System.exit(1);
		} else {
			Configuration conf = new Configuration();
			ToolRunner.run(conf, new SequenceFilesFromDirectory(), args);
		}
	}

}