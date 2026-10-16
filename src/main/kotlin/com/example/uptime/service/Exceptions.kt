package com.example.uptime.service

class MonitorNotFoundException(id: Long) : RuntimeException("Monitor $id not found")

class InvalidTargetException(message: String) : RuntimeException(message)
