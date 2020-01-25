package rdt;

public class Timer extends Thread {
	boolean timerFinished = false;
	int timerNumber = -1;
	int timerTime = 0;
	
	public Timer(int timerTime, int timerNumber) {
		this.timerTime = timerTime;
		this.timerNumber = timerNumber;
	}
	
	@Override
	public void run() {
		try {
			Thread.sleep(this.timerTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		this.timerFinished = true;
	}
}
