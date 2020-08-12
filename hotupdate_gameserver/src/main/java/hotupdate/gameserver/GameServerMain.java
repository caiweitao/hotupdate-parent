package hotupdate.gameserver;

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

					}
				}
			}
		}).start();
		Thread.sleep(3000); //主线程先暂停一会，形成对比效果
		HotUpdateClass.updateClass("TestHot");
	}

}
