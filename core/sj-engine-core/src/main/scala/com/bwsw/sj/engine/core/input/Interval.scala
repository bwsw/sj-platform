package com.bwsw.sj.engine.core.input

/**
 * Class represents an interval of buffer, which contains incoming data of input module,
 * that defines the boundaries of an input envelope
 * Created: 18/07/2016
 *
 * @author Kseniya Mikhaleva
 */
case class Interval(initialValue: Int, finalValue: Int)
