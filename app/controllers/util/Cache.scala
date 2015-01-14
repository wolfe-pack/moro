package controllers.util

import scala.collection.mutable

/**
 * @author sameer
 * @since 1/14/15.
 */
class Cache[A, B](val maxCacheSize: Int = 100) extends mutable.Map[A,B] {
  val _cache = new mutable.HashMap[A, B]
  val _queue = new mutable.Queue[A]()

  override def +=(kv: (A, B)) = {
    _cache += kv
    _queue += kv._1
    if (_cache.size > maxCacheSize) {
      // get rid of the oldest input
      val deleted = _queue.dequeue()
      _cache.remove(deleted)
      println("Too big, removing.. " + deleted)
    }
    this
  }

  override def -=(key: A) = {
    _cache -= key
    _queue.dequeueFirst(_ == key)
    this
  }

  override def get(key: A): Option[B] = _cache.get(key)

  override def iterator: Iterator[(A, B)] = _cache.iterator
}
