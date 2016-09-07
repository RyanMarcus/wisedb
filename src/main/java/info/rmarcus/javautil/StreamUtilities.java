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

import java.util.Iterator;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StreamUtilities {
	public static <T, V> Stream<Pair<T, V>> zip(Stream<T> s1, Stream<V> s2) {
		
		Iterator<T> it = s1.iterator();
		Iterator<V> iv = s2.iterator();
		
		Iterator<Pair<T, V>> pairs = new Iterator<Pair<T, V>>() {

			@Override
			public boolean hasNext() {
				return it.hasNext() && iv.hasNext();
			}

			@Override
			public Pair<T, V> next() {
				return new Pair<T, V>(it.next(), iv.next());
			}
			
		};
		
		Iterable<Pair<T, V>> itPairs = () -> pairs;
		
		return StreamSupport.stream(itPairs.spliterator(), false);
		
	}
	
	
	
	public static class Pair<T, V> {
		
		private T it;
		private V iv;
		
		public Pair(T t, V v) {
			this.it = t;
			this.iv = v;
		}
		
		
		public T getA() { return it; }
		public V getB() { return iv; }
		
		public <K> StreamUtilities.Pair<K, V> mutateA(Function<T, K> op) {
			return new Pair<K, V>(op.apply(getA()), getB());
		}
		
		public <K> StreamUtilities.Pair<T, K> mutateB(Function<V, K> op) {
			return new Pair<T, K>(getA(), op.apply(getB()));
		}
		
		
		@Override
		public String toString() {
			return "<" + getA() + ", " + getB() + ">";
		}
	}
}
