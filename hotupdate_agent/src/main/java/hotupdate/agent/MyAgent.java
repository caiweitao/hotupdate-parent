package hotupdate.agent;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;

public class MyAgent {

	/**
	 * 该方法支持在JVM 启动后再启动代理，对应清单的Agent-Class:属性
	 * @param path 在调用VirtualMachine类更新时,vm.loadAgent方法的第二个参数
	 * @param inst
	 */
	public static void agentmain(String path,Instrumentation inst){
		try{
			System.out.println("------------------ 进入 agentmain----------------------");
			System.out.println("传进来的参数为:"+path);
			File f = new File(path);
			byte[] targetClassFile = new byte[(int)f.length()];
			DataInputStream dis = new DataInputStream(new FileInputStream(f));
			dis.readFully(targetClassFile);
			dis.close();

			DynamicClassLoader myLoader = new DynamicClassLoader();
			Class<?> targetClazz = myLoader.findClass(targetClassFile);
			System.out.println("目标class类全路径为:"+targetClazz.getName());
			ClassDefinition clazzDef = new ClassDefinition(Class.forName(targetClazz.getName()), targetClassFile);
			inst.redefineClasses(clazzDef);

			System.out.println("重新定义["+path+"]完成！！");
			System.out.println("------------------ 退出 agentmain----------------------");
		}catch(Exception e){
			e.printStackTrace();
		}
	}
}
