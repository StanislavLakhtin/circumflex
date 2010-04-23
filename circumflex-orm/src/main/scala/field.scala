package ru.circumflex.orm

import ru.circumflex.core.WrapperModel

// ## Value Holder

/**
 * *Value holder* is designed to be an extensible atomic data carrier unit
 * of record. It is subclassed by 'Field' and 'Association'.
 */
trait ValueHolder[R <: Record[R], T] extends WrapperModel {

  // An internally stored value.
  protected var _value: T = _

  /**
   * An enclosing record.
   */
  def record: R

  // This way the value will be unwrapped by FTL engine.
  def item = getValue

  // Accessors and mutators.

  def getValue(): T = _value
  def apply(): T = getValue

  def setValue(newValue: T): this.type = {
    _value = newValue
    return this
  }
  def :=(newValue: T): this.type = setValue(T)
  def update(newValue: T): this.type = setValue(T)

  /**
   * Return a `String` representation of internal value.
   */
  def toString(default: String) = if (getValue == null) default else getValue.toString

  override def toString = toString("")
}

// ## Field

/**
 * Each field of persistent class correspond to a field of record in a relation.
 * We strongly distinguish `NULLABLE` and `NOT_NULL` fields.
 */
abstract class Field[R <: Record[R], T](val record: R,
                                                val sqlType: String)
    extends ValueHolder[R, T] {

  // Should the `UNIQUE` constraint be generated for this field?
  protected var _unique: Boolean = false
  def unique: this.type = {
    _unique = true
    return this
  }
  def UNIQUE: this.type = unique
  def unique_?() = _unique

  // An optional default expression for DDL.
  protected[orm] var _default: Option[String] = None
  def default = _default
  def default(expr: String): this.type = {
    _default = Some(expr)
    this
  }
  def DEFAULT = default
  def DEFAULT(expr: String): this.type = default(expr)

}

class NotNullField[R <: Record[R], T](r: R, t: String)
    extends Field[R, T](r, t) {

  def nullable: NullableField[R, T] = {
    val c = new NullableField[R, T](record, sqlType)
    c._default = this.default
    return c
  }
  def NULLABLE = nullable

  def notNull: NotNullField[R, T] = this
  def NOT_NULL = notNull

}

class NullableField[R <: Record[R], T](r: R, t: String)
    extends Field[R, Option[T]](r, t) {

  def get(): T = _value.get
  def getOrElse(default: T): T = apply().getOrElse(default)

  def null_!() = setValue(None)
  def NULL_!() = null_!

  def nullable: NullableField[R, T] = this
  def NULLABLE = nullable

  def notNull: NotNullField[R, T] = {
    val c = new NotNullField[R, T](record, sqlType)
    c._default = this.default
    return c
  }
  def NOT_NULL = notNull

  override def toString(default: String) = apply() match {
    case Some(value) if value != null => value.toString
    case _ => default
  }
}