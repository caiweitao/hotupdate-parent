package hotupdate.gameserver;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.management.ManagementFactory;

import com.sun.tools.attach.VirtualMachine;

public class HotUpdateClass {
	
	private static final String agentJar = "hotupdate/agent/hotupdate_agent-0.0.1-SNAPSHOT.jar";//代理类 
	private static final String newClassPath = "hotupdate/class/";// 新类所在文件夹默认为：hotupdate/class/

	/**
	 * 更新class文件
	 * @param newClassPath 新class所在文件目录
	 * @param className 类名
	 */
	public static boolean updateClass (String newClassPath,String className) {
		try{
			String[] classStr = className.split("\\.");
			String path = newClassPath+classStr[classStr.length-1]+".class";
			
			// 判断文件是否存在
			File file = new File(path);
			if (!file.exists()) {
				throw new FileNotFoundException(path+" (系统找不到指定的文件。)");
			}
			
			//拿到当前jvm的进程id
			String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
			System.out.println("PID:"+pid);
			VirtualMachine vm = VirtualMachine.attach(pid);
			System.out.println("path=="+path);
			vm.loadAgent(agentJar, path);//path参数即agentmain()方法的第一个参数
			return true;
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean updateClass (String className) {
		return updateClass(newClassPath, className);
	}
}
