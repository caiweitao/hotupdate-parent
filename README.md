# hotupdate-parent
Java 开发领域，热更新一直是一个难以解决的问题。
目前用的是 **java.lang.instrument** 的 **agentmain** 方法来实现，该方法可以实现运行时动态更新class文件，但是也有局限性，它只能修改方法级别的更新，对类结构的改变（增加属性或方法）还是要重启。

## 一般有三部分（工程）组成 ##

### 代理工程（agentmain方法所在工程）。 ###
必须单独创建一个Java工程，后面是需要打包成单独的 Jar 包,放到执行更新的逻辑程序中，让其调用。

### 执行更新的逻辑程序 ###
可以单独一个程序，也可以跟目标程序放在同个工程,我这里就是跟目标工程放在一起。

### 目标程序 ###
也就是我们要热更的程序

## 一、代理工程(hotupdate_agent) ##

### 1.定义代理类 ###
    public class MyAgent {

	/**
	 * @param path 在调用VirtualMachine类更新时,vm.loadAgent方法的第二个参数
	 * @param inst
	 */
		public static void agentmain(String path,Instrumentation inst){
			try{
				System.out.println("------------------ 进入 agentmain----------------------");
				System.out.println("传进来的参数为:"+path);
				// 通过文件路径，读取 class 到byte[]
				File f = new File(path);
				byte[] targetClassFile = new byte[(int)f.length()];
				DataInputStream dis = new DataInputStream(new FileInputStream(f));
				dis.readFully(targetClassFile);
				dis.close();
				// 自定义的类加载器
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

### 2.自定义类加载器： ###

    public class DynamicClassLoader extends ClassLoader {
		public Class<?> findClass(byte[] b) throws ClassNotFoundException { 
			return defineClass(null, b, 0, b.length); 
		}
	}

### 3.编写 MANIFEST.MF ###
在pom.xml中加入：

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-jar-plugin</artifactId>
                <version>2.3.1</version>
                <configuration>
                    <archive>
                        <manifest>
                            <addClasspath>true</addClasspath>
                        </manifest>
                        <manifestEntries>
						<!--指定 agentmain 方法所在的类路径-->
                            <Agent-Class>
                                hotupdate.agent.MyAgent
                            </Agent-Class>
						<!--是否能重定义此代理所需的类-->
                            <Can-Redefine-Classes>true</Can-Redefine-Classes>
						<!--是否能重新转换此代理所需的类-->
                            <Can-Retransform-Classes>true</Can-Retransform-Classes>
                        </manifestEntries>
                    </archive>
                </configuration>
            </plugin>
        </plugins>
    </build>

打包后会自动生成 MANIFEST.MF 文件，如下：

    Manifest-Version: 1.0
	Archiver-Version: Plexus Archiver
	Created-By: Apache Maven
	Built-By: Admin
	Build-Jdk: 1.7.0_55
	Agent-Class: hotupdate.agent.MyAgent
	Can-Redefine-Classes: true
	Can-Retransform-Classes: true

最后，将该工程打包成jar，放到“更新程序”的工程里面（这里是目标程序）
**hotupdate/agent/hotupdate_agent-0.0.1-SNAPSHOT.jar**

![Image text](https://github.com/caiweitao/img-folder/blob/master/hotupdate/hotupdate_gameserver.png)

## 二、更新程序（hotupdate_gameserver） ##

    public class HotUpdateClass {
	
	// 代理类路径 
	private static final String agentJar = "hotupdate/agent/hotupdate_agent-0.0.1-SNAPSHOT.jar";
	// 新类所在文件夹默认为：hotupdate/class/
	private static final String newClassPath = "hotupdate/class/";

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
			//path参数即agentmain()方法的第一个参数
			vm.loadAgent(agentJar, path);
			return true;
		} catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}

这个函数就是在我们想要热更的时候执行的。

## 三、目标程序（hotupdate_gameserver） ##
#### 创建一个目标工程来测试代码 ####

    public class TestHot {
		@Override
		public String toString() {
			return "热更后：222222222";
		}
	}

#### 先把TestHot更新后的编译好放在 **hotupdate/class/** 目录下。再把它改一下做更新前用 ####

     public class TestHot {
		@Override
		public String toString() {
			return "热更前：111111111";
		}
	}
#### 最后编写测试主程序测试一下 ####

    public class GameServerMain {
		public static void main(String[] args) throws InterruptedException {
			final TestHot t = new TestHot();  //内存只有一个实例对象
			new Thread(new Runnable(){
				@Override
				public void run() {
					while(true){
						try{
							Thread.sleep(1000);
							System.err.println(t);
						}catch(Exception e){
							e.printStackTrace();
						}
					}
				}
			}).start();
			Thread.sleep(2000); //主线程先暂停一会，形成对比效果
			// 执行更新代码
			HotUpdateClass.updateClass("TestHot");
		}
	}

看一下效果：

![Image text](https://raw.githubusercontent.com/caiweitao/img-folder/master/hotupdate/test_result.png)