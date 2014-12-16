package scala.spores

import scala.language.experimental.macros
import scala.reflect.macros.Context

import scala.pickling._


object SporePickler {
  /*implicit*/
  def genSporePickler[T, R, U](implicit tag: FastTypeTag[Spore[T, R]], format: PickleFormat,
                                        cPickler: SPickler[U], cUnpickler: Unpickler[U], cTag: FastTypeTag[U])
        : SPickler[Spore[T, R] { type Captured = U }] = macro genSporePicklerImpl[T, R, U]

  def genSporePicklerImpl[T: c.WeakTypeTag, R: c.WeakTypeTag, U: c.WeakTypeTag]
        (c: Context)(tag: c.Expr[FastTypeTag[Spore[T, R]]], format: c.Expr[PickleFormat], cPickler: c.Expr[SPickler[U]], cUnpickler: c.Expr[Unpickler[U]], cTag: c.Expr[FastTypeTag[U]]): c.Expr[SPickler[Spore[T, R] { type Captured = U }]] = {
    import c.universe._
    import definitions.ArrayClass

    val ttpe = weakTypeOf[T]
    val rtpe = weakTypeOf[R]
    val utpe = weakTypeOf[U]

    def isEffectivelyPrimitive(tpe: c.Type): Boolean = tpe match {
      case TypeRef(_, sym: ClassSymbol, _) if sym.isPrimitive => true
      case TypeRef(_, sym, eltpe :: Nil) if sym == ArrayClass && isEffectivelyPrimitive(eltpe) => true
      case _ => false
    }

    //TODO: check if U is a tuple

    //val numVarsCaptured = utpe.typeArgs.size
    // println(s"numVarsCaptured = $numVarsCaptured")
    val sporeTypeName = newTypeName("SporeC1")

    debug(s"T: $ttpe, R: $rtpe, U: $utpe")

    val picklerUnpicklerName = c.fresh(newTermName("SporePicklerUnpickler"))

    c.Expr[SPickler[Spore[T, R] { type Captured = U }]](q"""
      val capturedPickler = $cPickler
      val capturedUnpickler = $cUnpickler
      object $picklerUnpicklerName extends scala.pickling.SPickler[Spore[$ttpe, $rtpe] { type Captured = $utpe }]
          with scala.pickling.Unpickler[Spore[$ttpe, $rtpe] { type Captured = $utpe }] {
        val format = null // unused

        def pickle(picklee: Spore[$ttpe, $rtpe] { type Captured = $utpe }, builder: PBuilder): Unit = {
          builder.beginEntry(picklee)

          builder.putField("className", b => {
            b.hintTag(implicitly[FastTypeTag[String]])
            b.hintStaticallyElidedType()
            scala.pickling.SPickler.stringPicklerUnpickler.pickle(picklee.className, b)
          })

          builder.putField("c1", b => {
          	b.hintTag($cTag)
          	${if (isEffectivelyPrimitive(utpe)) q"b.hintStaticallyElidedType()" else q""}
            capturedPickler.pickle(picklee.asInstanceOf[$sporeTypeName[$ttpe, $rtpe]].c1.asInstanceOf[$utpe], b)
          })

          builder.endEntry()
        }

        def unpickle(tag: => scala.pickling.FastTypeTag[_], reader: PReader): Any = {
          val reader1 = reader.readField("tpe")
          reader1.hintTag(implicitly[FastTypeTag[String]])
          reader1.hintStaticallyElidedType()

          val tag = reader1.beginEntry()
          val result1 = scala.pickling.SPickler.stringPicklerUnpickler.unpickle(tag, reader1)
          reader1.endEntry()

          val reader2 = reader.readField("className")
          reader2.hintTag(implicitly[FastTypeTag[String]])
          reader2.hintStaticallyElidedType()

          val tag2 = reader2.beginEntry()
          val result = scala.pickling.SPickler.stringPicklerUnpickler.unpickle(tag2, reader2)
          reader2.endEntry()

          println("creating instance of class " + result)
          val clazz = java.lang.Class.forName(result.asInstanceOf[String])
          val sporeInst = scala.concurrent.util.Unsafe.instance.allocateInstance(clazz).asInstanceOf[$sporeTypeName[$ttpe, $rtpe] { type Captured = $utpe }]

          val reader3 = reader.readField("c1")
          reader3.hintTag($cTag)
          ${if (isEffectivelyPrimitive(utpe)) q"reader3.hintStaticallyElidedType()" else q""}
          val tag3 = reader3.beginEntry()
          val result3 = capturedUnpickler.unpickle(tag3, reader3)
          reader3.endEntry()
          sporeInst.c1 = result3.asInstanceOf[sporeInst.C1]

          sporeInst
        }
      }
      $picklerUnpicklerName
    """)
  }

  // TODO: probably need also implicit macro for SPickler[Spore[T, R] { type Captured = U }]
  // capture > 1 variables
  implicit def genSporeCMPickler[T, R, U <: Product](implicit tag: FastTypeTag[Spore[T, R]], format: PickleFormat,
                                                              cPickler: SPickler[U], cUnpickler: Unpickler[U], cTag: FastTypeTag[U])
        : SPickler[SporeWithEnv[T, R] { type Captured = U; val className: String }] = macro genSporeCMPicklerImpl[T, R, U]

  def genSporeCMPicklerImpl[T: c.WeakTypeTag, R: c.WeakTypeTag, U: c.WeakTypeTag]
        (c: Context)(tag: c.Expr[FastTypeTag[Spore[T, R]]], format: c.Expr[PickleFormat], cPickler: c.Expr[SPickler[U]], cUnpickler: c.Expr[Unpickler[U]], cTag: c.Expr[FastTypeTag[U]]): c.Expr[SPickler[SporeWithEnv[T, R] { type Captured = U; val className: String }]] = {
    import c.universe._
    import definitions.ArrayClass

    // println("enter genSporeCMPicklerImpl...")

    val ttpe = weakTypeOf[T]
    val rtpe = weakTypeOf[R]
    val utpe = weakTypeOf[U]

    // assert(utpe.typeArgs.size > 1)

    def isEffectivelyPrimitive(tpe: c.Type): Boolean = tpe match {
      case TypeRef(_, sym: ClassSymbol, _) if sym.isPrimitive => true
      case TypeRef(_, sym, eltpe :: Nil) if sym == ArrayClass && isEffectivelyPrimitive(eltpe) => true
      case _ => false
    }

    //TODO: check if U is a tuple

    //val numVarsCaptured = utpe.typeArgs.size
    // println(s"numVarsCaptured = $numVarsCaptured")
    val sporeTypeName = newTypeName("SporeWithEnv")

    debug(s"T: $ttpe, R: $rtpe, U: $utpe")

    val picklerUnpicklerName = c.fresh(newTermName("SporePicklerUnpickler"))

    c.Expr[SPickler[SporeWithEnv[T, R] { type Captured = U; val className: String }]](q"""
      val capturedPickler = $cPickler
      val capturedUnpickler = $cUnpickler
      object $picklerUnpicklerName extends scala.pickling.SPickler[SporeWithEnv[$ttpe, $rtpe] { type Captured = $utpe; val className: String }]
          with scala.pickling.Unpickler[Spore[$ttpe, $rtpe]] {
        val format = null // unused
        def pickle(picklee: SporeWithEnv[$ttpe, $rtpe] { type Captured = $utpe; val className: String }, builder: PBuilder): Unit = {
          builder.beginEntry(picklee)

          builder.putField("className", b => {
            b.hintTag(implicitly[FastTypeTag[String]])
            b.hintStaticallyElidedType()
            scala.pickling.SPickler.stringPicklerUnpickler.pickle(picklee.className, b)
          })

          builder.putField("captured", b => {
            b.hintTag($cTag)
            ${if (isEffectivelyPrimitive(utpe)) q"b.hintStaticallyElidedType()" else q""}
            capturedPickler.pickle(picklee.asInstanceOf[$sporeTypeName[$ttpe, $rtpe]].captured.asInstanceOf[$utpe], b)
          })

          builder.endEntry()
        }

        def unpickle(tag: => scala.pickling.FastTypeTag[_], reader: PReader): Any = {
          val reader1 = reader.readField("tpe")
          reader1.hintTag(implicitly[FastTypeTag[String]])
          reader1.hintStaticallyElidedType()

          val tag = reader1.beginEntry()
          val result1 = scala.pickling.SPickler.stringPicklerUnpickler.unpickle(tag, reader1)
          reader1.endEntry()

          val reader2 = reader.readField("className")
          reader2.hintTag(implicitly[FastTypeTag[String]])
          reader2.hintStaticallyElidedType()

          val tag2 = reader2.beginEntry()
          val result = scala.pickling.SPickler.stringPicklerUnpickler.unpickle(tag2, reader2)
          reader2.endEntry()

          println("creating instance of class " + result)
          val clazz = java.lang.Class.forName(result.asInstanceOf[String])
          val sporeInst = scala.concurrent.util.Unsafe.instance.allocateInstance(clazz).asInstanceOf[$sporeTypeName[$ttpe, $rtpe] { type Captured = $utpe }]

          val reader3 = reader.readField("captured")
          reader3.hintTag($cTag)
          ${if (isEffectivelyPrimitive(utpe)) q"reader3.hintStaticallyElidedType()" else q""}
          val tag3 = reader3.beginEntry()
          val result3 = capturedUnpickler.unpickle(tag3, reader3)
          reader3.endEntry()
          sporeInst.captured = result3.asInstanceOf[sporeInst.Captured]

          sporeInst
        }
      }
      $picklerUnpicklerName
    """)
  }

  implicit def genSporeUnpickler[T, R](implicit format: PickleFormat): Unpickler[Spore[T, R]] =
    macro genSporeUnpicklerImpl[T, R]

  def genSporeUnpicklerImpl[T: c.WeakTypeTag, R: c.WeakTypeTag](c: Context)(format: c.Expr[PickleFormat]): c.Expr[Unpickler[Spore[T, R]]] = {
    import c.universe._
    import definitions.ArrayClass

    // println("enter genSporeUnpicklerImpl...")

    val ttpe = weakTypeOf[T]
    val rtpe = weakTypeOf[R]

    def isEffectivelyPrimitive(tpe: c.Type): Boolean = tpe match {
      case TypeRef(_, sym: ClassSymbol, _) if sym.isPrimitive => true
      case TypeRef(_, sym, eltpe :: Nil) if sym == ArrayClass && isEffectivelyPrimitive(eltpe) => true
      case _ => false
    }

    debug(s"T: $ttpe, R: $rtpe")

    val unpicklerName = c.fresh(newTermName("SporeUnpickler"))

    c.Expr[Unpickler[Spore[T, R]]](q"""
      object $unpicklerName extends scala.pickling.Unpickler[Spore[$ttpe, $rtpe]] {
        val format = null // unused
        def unpickle(tag: => scala.pickling.FastTypeTag[_], reader: PReader): Any = {
          val reader2 = reader.readField("className")
          reader2.hintTag(implicitly[FastTypeTag[String]])
          reader2.hintStaticallyElidedType()

          val tag2 = reader2.beginEntry()
          val result = scala.pickling.SPickler.stringPicklerUnpickler.unpickle(tag2, reader2)
          reader2.endEntry()

          println("creating instance of class " + result)
          val clazz = java.lang.Class.forName(result.asInstanceOf[String])
          val sporeInst = scala.concurrent.util.Unsafe.instance.allocateInstance(clazz).asInstanceOf[SporeWithEnv[$ttpe, $rtpe]]

          val reader3 = reader.readField("captured")
          val tag3 = reader3.beginEntry()
          val value = {
            if (reader3.atPrimitive) {
              reader3.readPrimitive()
            } else {
              val unpickler3 = scala.pickling.Unpickler.genUnpickler(scala.reflect.runtime.currentMirror, tag3)
              unpickler3.unpickle(tag3, reader3)
            }
          }
          reader3.endEntry()
          sporeInst.captured = value.asInstanceOf[sporeInst.Captured]
          sporeInst
        }
      }
      $unpicklerName
    """)
  }

}
