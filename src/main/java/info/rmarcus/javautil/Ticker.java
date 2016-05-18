// { begin copyright } 
// Copyright Ryan Marcus 2016
// 
// This file is part of WiSeDB.
// 
// WiSeDB is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
// 
// WiSeDB is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
// 
// You should have received a copy of the GNU General Public License
// along with WiSeDB.  If not, see <http://www.gnu.org/licenses/>.
// 
// { end copyright } 
 

package info.rmarcus.javautil;

public class Ticker implements Runnable {

	
	private Tickable t;
	private int delay;
	
	
	public Ticker(Tickable t) {
		this(t, 100);
	}
	
	public Ticker(Tickable t, int delay) {
		this.t = t;
		this.delay = delay;
	}
	
	public void stop() {
		t = null;
	}
	
	@Override
	public void run() {
		while (t != null) {
			synchronized (t) {
				if (t != null) {
					t.tick();
				}
			}
			
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
				// TODO
			}
		}

	}
	
	public void runInThread() {
		new Thread(this).start();
	}

}
