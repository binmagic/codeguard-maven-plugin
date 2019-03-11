package magic.guard;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.*;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;

/**
 * @author xubao
 * @version 1.0
 * @since 2019/3/4
 */

@Mojo(name = "guard", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true)
public class CodeGuardMoJo extends AbstractMojo
{
	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	@Parameter
	private boolean removeTempOutDir = true;

	@Parameter
	private String injar;

	@Parameter
	private String outjar;

	@Parameter(defaultValue = "${project.build.directory}")
	private File outputDirectory;

	@Parameter(defaultValue = "${project.build.directory}")
	private File inputDirectory;

	@Parameter
	private Set<String> includes;
	@Parameter
	private Set<String> excludes;

	@Parameter
	private boolean skip;

	@Parameter
	private String tempOutDirName = "temp_guard";

	public native static byte[] encrypt(byte[] text);

	// for test
//	public static byte[] encrypt(byte[] text)
//	{
//		return text;
//	}

	private void load()
	{
		LibUtils.setLog(getLog());

		String path = LibUtils.exportLib("libjcencryption");
		getLog().debug("lib暂存path: " + path);
		System.load(path);
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		load();

		if(project.getPackaging().equals("pom"))
		{
			getLog().info("以pom打包,跳过!");
			return;
		}

		if(skip)
		{
			getLog().info("跳过加密");
			return;
		}

		if(includes == null)
		{
			includes = new HashSet<>(Arrays.asList(".*\\.class"));
		}

		RegMatcher include = new RegMatcher(includes);
		RegMatcher exclude = new RegMatcher(excludes);

		File inJarFile = new File(inputDirectory, injar);
		if(!inJarFile.exists())
		{
			throw new MojoFailureException("Can't find file " + inJarFile);
		}

		if(!outputDirectory.exists())
		{
			if(!outputDirectory.mkdirs())
			{
				throw new MojoFailureException("Can't create " + outputDirectory);
			}
		}

		File outJarFile = new File(outputDirectory, outjar);

		if(inJarFile.equals(outJarFile))
		{
			throw new MojoExecutionException("输入jar,输出jar,路径相同:" + inJarFile.getAbsolutePath());
		}


		JarFile jarFile;
		try
		{
			jarFile = new JarFile(inJarFile);
		}
		catch(IOException e)
		{
			throw new MojoExecutionException("打开jar文件: " + inJarFile + " 失败!", e);
		}

		File tempOutDir = new File(outputDirectory, tempOutDirName);
		if(tempOutDir.exists())
		{
			try
			{
				FileUtils.deleteDirectory(tempOutDir);
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
		tempOutDir.mkdir();

		try
		{
			doGuardForJar(jarFile, tempOutDir, include, exclude);
		}
		catch(IOException e)
		{
			throw new MojoExecutionException("对jar进行加密失败", e);
		}

		try
		{
			rePackageJar(tempOutDir, outJarFile);
		}
		catch(IOException e)
		{
			throw new MojoExecutionException("重新打包成jar失败", e);
		}

		//移除临时文件夹
		if(removeTempOutDir)
		{
			try
			{
				FileUtils.deleteDirectory(tempOutDir);
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}

	}

	private void doGuardForJar(JarFile inJar, File outDir, RegMatcher include, RegMatcher exclude) throws MojoExecutionException, IOException
	{
		if(!outDir.exists())
		{
			throw new MojoExecutionException("文件夹不存在:" + outDir.getAbsolutePath());
		}

		Enumeration<JarEntry> entries = inJar.entries();

		ByteArrayOutputStream byteBuff = new ByteArrayOutputStream();

		while(entries.hasMoreElements())
		{
			JarEntry entry = entries.nextElement();

			if(entry.isDirectory())
			{
				File dir = new File(outDir.getAbsolutePath() + "//" + entry.getName());
				if(!dir.exists())
				{
					if(!dir.mkdirs())
					{
						throw new MojoExecutionException("创建文件夹失败: " + dir.getAbsolutePath());
					}
				}
			}
			else
			{
				ZipEntry zipEntry = inJar.getEntry(entry.getName());
				InputStream is = inJar.getInputStream(zipEntry);

				String tempPath = outDir.getAbsolutePath() + "//" + entry.getName();
				File dir = new File(tempPath.substring(0, tempPath.lastIndexOf("/")));
				if(!dir.exists())
				{
					if(!dir.mkdirs())
					{
						throw new MojoExecutionException("创建文件夹失败: " + dir.getAbsolutePath());
					}
				}
				File tempFile = new File(tempPath);
				//默认不包含的文件不加密
				boolean guard = false;

				if(include != null)
				{
					//包含的文件如果没有排除则加密
					if(include.match(entry.getName()))
					{
						guard = true;
					}
				}

				if(exclude != null)
				{
					//排除的文件一定不加密
					if(exclude.match(entry.getName()))
					{
						guard = false;
					}
				}

				if(guard)
				{
					if(getLog().isDebugEnabled())
						getLog().debug(entry.getName() + " 加密");
					byteBuff.reset();
					IOUtils.copy(is, byteBuff);

					byte[] bytes = doGuardForData(entry.getName(), byteBuff.toByteArray());

					FileUtils.writeByteArrayToFile(tempFile, bytes);
				}
				else
				{
					if(getLog().isDebugEnabled())
						getLog().debug(entry.getName() + " 不加密");
					FileUtils.copyToFile(is, tempFile);
				}
			}

		}
	}

	private byte[] doGuardForData(String filePath, byte[] fileData) throws MojoExecutionException
	{
		byte[] encrypt = null;
		try
		{
			getLog().debug("加密输入字节:" + fileData.length);
			encrypt = CodeGuardMoJo.encrypt(fileData);
			getLog().debug("加密输出字节:" + encrypt.length);
		}
		catch(Exception e)
		{
			throw new MojoExecutionException("加密出现异常", e);
		}
		return encrypt;
	}

	private void rePackageJar(File rootDir, File outJar) throws IOException
	{
		if(!outJar.exists())
		{
			outJar.createNewFile();
		}

		FileOutputStream fos = new FileOutputStream(outJar);
		CheckedOutputStream cos = new CheckedOutputStream(fos,
				new CRC32());
		JarOutputStream jos = new JarOutputStream(cos);
		compass(jos, rootDir, null);
		jos.close();
		cos.close();
		fos.close();
	}


	private void compass(JarOutputStream jos, File parentDir, String dir) throws IOException
	{
		if(dir == null)
		{
			dir = "";
		}
		if(!parentDir.exists())
		{
			return;
		}
		File[] files = parentDir.listFiles();
		for(File file : files)
		{
			if(file.isDirectory())
			{
				jos.putNextEntry(new JarEntry(dir + file.getName() + "/"));
				compass(jos, file, dir + file.getName() + "/");
			}
			else
			{
				jos.putNextEntry(new JarEntry(dir + file.getName()));
				FileUtils.copyFile(file, jos);
			}
		}

	}


}
