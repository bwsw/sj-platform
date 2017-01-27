package com.bwsw.common.jdbc

/**
   * Builder class for JDBC client
   */
object JdbcClientBuilder {
   private var jdbcClientConnectionData = new JdbcClientConnectionData()

   def buildCheck() = {
     jdbcClientConnectionData.database match {
       case ""|null => throw new RuntimeException("database field must be declared.")
       case _:String =>
     }
     jdbcClientConnectionData.driver match {
       case ""|null => throw new RuntimeException("driver field must be declared.")
       case _:String =>
     }
     jdbcClientConnectionData.table match {
//       case ""|null => throw new RuntimeException("table field must be declared.")
       case _:String =>
     }
     jdbcClientConnectionData.txnField match {
       case ""|null => throw new RuntimeException("txnField field must be declared.")
       case _:String =>
     }
     jdbcClientConnectionData.username match {
       case ""|null => throw new RuntimeException("username field must be declared.")
       case _:String =>
     }
     jdbcClientConnectionData.password match {
       case ""|null => throw new RuntimeException("password field must be declared.")
       case _:String =>
     }
     jdbcClientConnectionData.hosts match {
       case null => throw new RuntimeException("hosts field must be declared.")
       case _ =>
     }
   }

   def build(): JdbcClient = {
     buildCheck()
     new JdbcClient(jdbcClientConnectionData)
   }

   def setHosts(hosts: Array[String]) = {jdbcClientConnectionData.hosts=hosts; this}
   def setDriver(driver: String) = {jdbcClientConnectionData.driver=driver; this}
   def setUsername(username: String) = {jdbcClientConnectionData.username=username; this}
   def setPassword(password: String) = {jdbcClientConnectionData.password=password; this}
   def setDatabase(database: String) = {jdbcClientConnectionData.database=database; this}
   def setTable(table: String) = {jdbcClientConnectionData.table=table; this}
   def setTxnField(txnField:String) = {jdbcClientConnectionData.txnField=txnField; this}

   /**
     * Use this method if you have JDBC connection data provider
     *
     * @param jdbcClientConnectionData: JdbcClientConnectionData
     * @return this
     */
   def setJdbcClientConnectionData(jdbcClientConnectionData: JdbcClientConnectionData) = {
     this.jdbcClientConnectionData = jdbcClientConnectionData; this
   }
 }
