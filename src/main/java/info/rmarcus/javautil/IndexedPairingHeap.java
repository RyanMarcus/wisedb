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

import java.util.AbstractQueue;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import info.rmarcus.javautil.PairingHeap.Position;

public class IndexedPairingHeap<K extends Comparable<K>> extends AbstractQueue<K> {

	private Map<K, Position<K>> index;
	private PairingHeap<K> heap;
	
	public IndexedPairingHeap() {
		index = new HashMap<K, Position<K>>();
		heap = new PairingHeap<K>();
	}

	
	public void updateItem(K e) {
		Position<K> p = index.get(e);
		
		if (e == null) {
			throw new IllegalArgumentException("Tried to update an item that is not in the heap!"); 
		}
		
		if (p.getValue().compareTo(e) < 0)
			throw new IllegalArgumentException("Can only update keys with smaller values than their original value");
		
		heap.decreaseKey(p, e);
	}
	
	@Override
	public boolean contains(Object e) {
		return index.containsKey(e);
	}
	
	public K getValue(K e) {
		return index.get(e).getValue();
	}
	
	@Override
	public boolean offer(K e) {
		Position<K> p = heap.insert(e);
		index.put(e, p);
		return true;
	}

	@Override
	public K poll() {
		if (heap.isEmpty())
			return null;
		
		K item = heap.deleteMin();
		index.remove(item);
		return item;
	}

	@Override
	public K peek() {
		if (heap.isEmpty())
			return null;
		
		return heap.findMin();
	}

	@Override
	public Iterator<K> iterator() {
		return index.keySet().iterator();
	}

	@Override
	public int size() {
		return heap.size();
	}
	
	
}
