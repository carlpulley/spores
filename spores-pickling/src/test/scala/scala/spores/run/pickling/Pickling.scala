package scala.spores
package run
package pickling

import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

import scala.pickling._
import json._

import SporePickler._

@RunWith(classOf[JUnit4])
class PicklingSpec {
  @Test
  def `pickle/unpickle to/from JSON`() {
    val v1 = 10
    val s = spore {
      val c1 = v1
      (x: Int) => s"arg: $x, c1: $c1"
    }

    val pickler: SPickler[Spore[Int, String] { type Captured = Int }] with Unpickler[Spore[Int, String] { type Captured = Int }] =
      SporePickler.genSporePickler[Int, String, Int]

    val format = implicitly[PickleFormat]
    val builder = format.createBuilder

    builder.hintTag(implicitly[FastTypeTag[Spore[Int, String]]])
    pickler.pickle(s, builder)
    val res = builder.result()

    assert(res.value.toString.endsWith("""
  "c1": 10
}"""))

    val reader = format.createReader(res.asInstanceOf[format.PickleType], scala.reflect.runtime.currentMirror)
    val up = pickler.unpickle(???, reader)
    val us = up.asInstanceOf[Spore[Int, String]]
    val res2 = us(5)
    assert(res2 == "arg: 5, c1: 10")
  }

  @Test
  def `simplified spore pickling`() {
    val v1 = 10
    val s = spore {
      val c1 = v1
      (x: Int) => s"arg: $x, c1: $c1"
    }

    val pickler = SporePickler.genSporePickler[Int, String, Int]
    val builder = implicitly[PickleFormat].createBuilder
    val res = {
      builder.hintTag(implicitly[FastTypeTag[Spore[Int, String]]])
      pickler.pickle(s, builder)
      builder.result()
    }

    assert(res.value.toString.endsWith("""
      |  "c1": 10
      |}""".stripMargin))
  }

  @Test
  def `pickling spore with two captured variables`() {
    val v1 = 10
    val v2 = "hello"
    val s = spore {
      val c1 = v1
      val c2 = v2
      (x: Int) => s"arg: $x, c1: $c1, c2: $c2"
    }

    val pickler = SporePickler.genSporeCMPickler[Int, String, (Int, String)]
    val format  = implicitly[PickleFormat]
    val builder = format.createBuilder
    val res = {
      builder.hintTag(implicitly[FastTypeTag[Spore[Int, String]]])
      pickler.pickle(s, builder)
      builder.result()
    }

    assert(res.value.toString.endsWith(""""captured": {
      |    "tpe": "scala.Tuple2[scala.Int,java.lang.String]",
      |    "_1": 10,
      |    "_2": "hello"
      |  }
      |}""".stripMargin))

    val reader = format.createReader(res.asInstanceOf[format.PickleType], scala.reflect.runtime.currentMirror)
    val up = pickler.unpickle(???, reader)
    val us = up.asInstanceOf[Spore[Int, String]]
    val res2 = us(5)
    assert(res2 == "arg: 5, c1: 10, c2: hello")
  }

  @Test
  def `simple pickling of spore with two captured variables`() {
    val v1 = 10
    val v2 = "hello"
    val s = spore {
      val c1 = v1
      val c2 = v2
      (x: Int) => s"arg: $x, c1: $c1, c2: $c2"
    }
    val res  = s.pickle
    val up   = res.unpickle[Spore[Int, String]]
    val res2 = up(5)
    assert(res2 == "arg: 5, c1: 10, c2: hello")
  }

  @Test
  def `simple pickling of spore with two parameters`() {
    val s = spore {
      (x: Int, s: String) => s"arg1: $x, arg2: $s"
    }
    val res  = s.pickle
    val up   = res.unpickle[Spore2[Int, String, String]]
    val res2 = up(5, "hi")
    assert(res2 == "arg1: 5, arg2: hi")
  }

  @Test
  def `simple pickling of spore with three parameters`() {
    val s = spore {
      (x: Int, s: String, c: Char) => s"arg1: $x, arg2: $s, arg3: $c"
    }
    val res  = s.pickle
    val up   = res.unpickle[Spore3[Int, String, Char, String]]
    val res2 = up(5, "hi", '-')
    assert(res2 == "arg1: 5, arg2: hi, arg3: -")
  }
}
