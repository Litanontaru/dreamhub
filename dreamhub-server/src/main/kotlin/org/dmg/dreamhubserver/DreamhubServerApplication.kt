package org.dmg.dreamhubserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class DreamhubServerApplication

fun main(args: Array<String>) {
	runApplication<DreamhubServerApplication>(*args)
}
