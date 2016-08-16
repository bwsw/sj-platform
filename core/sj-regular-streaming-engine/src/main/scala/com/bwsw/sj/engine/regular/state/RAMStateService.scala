package com.bwsw.sj.engine.regular.state

import java.util.UUID

import com.bwsw.common.ObjectSerializer
import com.bwsw.sj.engine.core.state.IStateService
import com.bwsw.sj.engine.regular.task.RegularTaskManager
import com.bwsw.tstreams.agents.consumer.ConsumerTransaction
import com.bwsw.tstreams.agents.consumer.Offsets.Oldest
import com.bwsw.tstreams.agents.group.CheckpointGroup
import com.bwsw.tstreams.agents.producer.NewTransactionProducerPolicy

import scala.collection.mutable

/**
 * Class representing a service for managing by a storage for default state that is kept in RAM and use t-stream for checkpoints
 * Created: 12/04/2016
 * @author Kseniya Mikhaleva
 *
 * @param manager Manager of environment of task of regular module
 * @param checkpointGroup Group of t-stream agents that have to make a checkpoint at the same time
 */

class RAMStateService(manager: RegularTaskManager, checkpointGroup: CheckpointGroup) extends IStateService {

  private val stateStreamName = manager.taskName + "_state"
  private val stateStream = createStateStream()
  /**
   * Producer is responsible for saving a partial changes of state or a full state
   */
  private val stateProducer = manager.createProducer(stateStream)

  /**
   * Consumer is responsible for retrieving a partial or full state
   */
  private val stateConsumer = manager.createConsumer(stateStream, List(0, 0), Oldest)
  stateConsumer.start()

  addAgentsToCheckpointGroup()

  /**
   * Number of a last transaction that saved a state. Used for saving partial changes of state
   */
  private var lastFullTxnUUID: Option[UUID] = None

  /**
   * Provides a serialization from a transaction data to a state variable or state change
   */
  private val serializer: ObjectSerializer = new ObjectSerializer()

  /**
   * Provides key/value storage to keep state changes. It's used to do checkpoint of partial changes of state
   */
  protected val stateChanges: mutable.Map[String, (String, Any)] = mutable.Map[String, (String, Any)]()

  /**
   * Provides key/value storage to keep state
   */
  private val stateVariables: mutable.Map[String, Any] = loadLastState()

  /**
   * Creates SJStream to keep a module state
   *
   * @return SjStream used for keeping a module state
   */
  private def createStateStream() = {
    logger.debug(s"Task name: ${manager.taskName} " +
      s"Get stream for keeping state of module\n")

    val description = "store state of module"
    val tags = Array("state")
    val partitions = 1

    manager.createTStreamOnCluster(stateStreamName, description, partitions)
    manager.getSjStream(stateStreamName, description, tags, partitions)
  }

  /**
   * Adds a state producer and a state consumer to checkpoint group
   */
  private def addAgentsToCheckpointGroup() = {
    logger.debug(s"Task: ${manager.taskName}. Start adding state consumer and producer to checkpoint group\n")
    checkpointGroup.add(stateConsumer)
    checkpointGroup.add(stateProducer)
    logger.debug(s"Task: ${manager.taskName}. Adding state consumer and producer to checkpoint group is finished\n")
  }

  /**
   * Allows getting last state. Needed for restoring after crashing
   * @return State variables
   */
  private def loadLastState(): mutable.Map[String, Any] = {
    logger.debug(s"Restore a state\n")
    val initialState = mutable.Map[String, Any]()
    val maybeTxn = stateConsumer.getLastTransaction(0)
    if (maybeTxn.nonEmpty) {
      logger.debug(s"Get txn that was last. It contains a full or partial state\n")
      val lastTxn = maybeTxn.get
      var value = serializer.deserialize(lastTxn.next())
      value match {
        case variable: (Any, Any) =>
          logger.debug(s"Last txn contains a full state\n")
          lastFullTxnUUID = Some(lastTxn.getTxnUUID)
          initialState(variable._1.asInstanceOf[String]) = variable._2
          fillFullState(initialState, lastTxn)
          initialState
        case _ =>
          logger.debug(s"Last txn contains a partial state. Start restoring it\n")
          lastFullTxnUUID = Some(value.asInstanceOf[UUID])
          val lastFullStateTxn = stateConsumer.getTransactionById(0, lastFullTxnUUID.get).get
          fillFullState(initialState, lastFullStateTxn)
          stateConsumer.setLocalOffset(0, lastFullTxnUUID.get)

          var maybeTxn = stateConsumer.getTransaction
          while (maybeTxn.nonEmpty) {
            val partialState = mutable.Map[String, (String, Any)]()
            val partialStateTxn = maybeTxn.get

            partialStateTxn.next()
            while (partialStateTxn.hasNext()) {
              value = serializer.deserialize(partialStateTxn.next())
              val variable = value.asInstanceOf[(String, (String, Any))]
              partialState(variable._1) = variable._2
            }
            applyPartialChanges(initialState, partialState)
            maybeTxn = stateConsumer.getTransaction
          }
          logger.debug(s"Restore of state is finished\n")
          initialState
      }
    } else {
      logger.debug(s"There was no one checkpoint of state\n")
      initialState
    }
  }

  /**
   * Check whether a state variable with specific key exists or not
   * @param key State variable name
   * @return True or false
   */
  override def isExist(key: String): Boolean = {
    logger.info(s"Check whether a state variable: $key  exists or not\n")
    stateVariables.contains(key)
  }

  /**
   * Gets a value of the state variable by key
   * @param key State variable name
   * @return Value of the state variable
   */
  override def get(key: String): Any = {
    logger.info(s"Get a state variable: $key\n")
    stateVariables(key)
  }

  /**
   * Puts a value of the state variable by key
   * @param key State variable name
   * @param value Value of the state variable
   */
  override def set(key: String, value: Any): Unit = {
    logger.info(s"Set a state variable: $key to $value\n")
    stateVariables(key) = value
  }

  /**
   * Delete a state variable by key
   * @param key State variable name
   */
  override def delete(key: String): Unit = {
    logger.info(s"Remove a state variable: $key\n")
    stateVariables.remove(key)
  }

  /**
   * Removes all state variables. After this operation has completed,
   * the state will be empty.
   */
  override def clear(): Unit = {
    logger.info(s"Remove all state variables\n")
    stateVariables.clear()
  }

  /**
   * Indicates that a state variable has been changed
   * @param key State variable name
   * @param value Value of the state variable
   */
  override def setChange(key: String, value: Any): Unit = {
    logger.info(s"Indicate that a state variable: $key value changed to $value\n")
    stateChanges(key) = ("set", value)
  }

  /**
   * Indicates that a state variable has been deleted
   * @param key State variable name
   */
  override def deleteChange(key: String): Unit = {
    logger.info(s"Indicate that a state variable: $key with value: ${stateVariables(key)} deleted\n")
    stateChanges(key) = ("delete", stateVariables(key))
  }

  /**
   * Indicates that all state variables have been deleted
   */
  override def clearChange(): Unit = {
    logger.info(s"Indicate that all state variables deleted\n")
    stateVariables.foreach(x => deleteChange(x._1))
  }

  override def savePartialState(): Unit = {
    logger.debug(s"Do checkpoint of a part of state\n")
    if (stateChanges.nonEmpty) {
      if (lastFullTxnUUID.isDefined) {
        sendChanges(lastFullTxnUUID.get, stateChanges)
        stateChanges.clear()
      } else saveFullState()
    }
  }

  override def saveFullState(): Unit = {
    logger.debug(s"Do checkpoint of a full state\n")
    if (stateVariables.nonEmpty) {
      lastFullTxnUUID = Some(sendState(stateVariables))
      stateChanges.clear()
    }
  }

  /**
   * Allow getting a state by gathering together all data from transaction
   * @param initialState State from which to need start
   * @param transaction Transaction containing a state
   */
  private def fillFullState(initialState: mutable.Map[String, Any], transaction: ConsumerTransaction[Array[Byte]]) = {
    logger.debug(s"Fill full state\n")
    var value: Object = null
    var variable: (String, Any) = null

    while (transaction.hasNext()) {
      value = serializer.deserialize(transaction.next())
      variable = value.asInstanceOf[(String, Any)]
      initialState(variable._1) = variable._2
    }
  }

  /**
   * Allows restoring a state consistently applying all partial changes of state
   * @param fullState Last state that has been saved
   * @param partialState Partial changes of state
   */
  private def applyPartialChanges(fullState: mutable.Map[String, Any], partialState: mutable.Map[String, (String, Any)]) = {
    logger.debug(s"Apply partial changes to state sequentially\n")
    partialState.foreach {
      case (key, ("set", value)) => fullState(key) = value
      case (key, ("delete", _)) => fullState.remove(key)
    }
  }

  /**
   * Does checkpoint of state
   * @param state State variables
   * @return UUID of transaction
   */
  private def sendState(state: mutable.Map[String, Any]): UUID = {
    logger.debug(s"Save a full state in t-stream intended for storing/restoring a state\n")
    val transaction = stateProducer.newTransaction(NewTransactionProducerPolicy.ErrorIfOpened)
    state.foreach((x: (String, Any)) => transaction.send(serializer.serialize(x)))
    transaction.getTransactionUUID()
  }

  /**
   * Does checkpoint of changes of state
   * @param uuid Transaction UUID for which a changes was applied
   * @param changes State changes
   */
  private def sendChanges(uuid: UUID, changes: mutable.Map[String, (String, Any)]) = {
    logger.debug(s"Save a partial state in t-stream intended for storing/restoring a state\n")
    val transaction = stateProducer.newTransaction(NewTransactionProducerPolicy.ErrorIfOpened)
    transaction.send(serializer.serialize(uuid))
    changes.foreach((x: (String, (String, Any))) => transaction.send(serializer.serialize(x)))
  }

  override def getNumberOfVariables: Int = {
    stateVariables.size
  }

  override def getAll: Map[String, Any] = stateVariables.toMap
}