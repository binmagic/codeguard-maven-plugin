package magic.guard;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @author xubao
 * @version 1.0
 * @since 2019/3/8
 */
public class LibUtils
{
	private static Log log;

	//BIN_LIB为JAR包中存放DLL的路径 "/" 表示classes目录
	public static final String BIN_LIB = "/lib/";
	public static Map<String, File> dllBuffMap = new HashMap<>();

	public static void setLog(Log log)
	{
		LibUtils.log = log;
	}

	private static boolean logEnable()
	{
		return log != null && log.isDebugEnabled();
	}

	//主要为打成jar包使用
	public synchronized static String copyLib(String libName)
	{
		String systemType = System.getProperty("os.name");
		if(logEnable())
		{
			log.debug("操作系统:" + systemType);
		}
		String low = systemType.toLowerCase();
		String libExtension;
		if(low.startsWith("mac"))
		{
			libExtension = ".dylib";
		}
		else if(low.startsWith("win"))
		{
			libExtension = ".dll";
		}
		//linux
		else
		{
			libExtension = ".so";
		}

		String libFullName = libName + libExtension;
		String libPath = BIN_LIB + libFullName;

		File lib_tmp = dllBuffMap.get(libFullName);
		if(lib_tmp == null || !lib_tmp.exists())
		{
			try
			{
				lib_tmp = File.createTempFile("lib_tmp", libExtension);
				dllBuffMap.put(libFullName, lib_tmp);
			}
			catch(IOException e)
			{
				throw new RuntimeException(e);
			}

			try(InputStream in = LibUtils.class.getResourceAsStream(libPath))
			{
				URL url = LibUtils.class.getResource(libPath);
				System.out.println(url);
				if(in != null)
				{
					FileUtils.copyToFile(in, lib_tmp);
				}
				else
				{
					throw new RuntimeException("没有找到" + libPath);
				}
			}
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
		return lib_tmp.exists() ? lib_tmp.getAbsolutePath() : null;
	}
}
