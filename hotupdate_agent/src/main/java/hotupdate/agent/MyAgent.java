package hotupdate.agent;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.util.Objects;

public class MyAgent {

	/**
	 * 该方法支持在JVM 启动后再启动代理，对应清单的Agent-Class:属性
	 * @param path 在调用VirtualMachine类更新时,vm.loadAgent方法的第二个参数
	 * @param inst
	 */
	public static void agentmain(String path,Instrumentation inst) throws Exception {
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
			String targetClassName = targetClazz.getName();
			System.out.println("目标class类全路径为:"+targetClassName);

			// 找出需要重新定义的类，非springboot打包的可用②方式
			Class<?> theClass = null;
			for (Class<?> c : inst.getAllLoadedClasses()) {
				if (c.getName().endsWith(targetClassName)) {
					theClass = c;
					break;
				}
			}
			Objects.requireNonNull(theClass);
			ClassDefinition clazzDef = new ClassDefinition(theClass, targetClassFile);
			// ② springboot打包用此方法会报ClassNotFoundException，目前只用上面的方法替代
			// ClassDefinition clazzDef = new ClassDefinition(Class.forName(targetClassName), targetClassFile);
			inst.redefineClasses(clazzDef);

			System.out.println("重新定义["+path+"]完成！！");
			System.out.println("------------------ 退出 agentmain----------------------");
		}catch(Exception e){
			e.printStackTrace();
			throw e;
		}
	}
}
