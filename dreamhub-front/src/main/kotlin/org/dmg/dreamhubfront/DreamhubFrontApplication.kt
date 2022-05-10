package org.dmg.dreamhubfront

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication
@EnableFeignClients
class DreamhubFrontApplication

fun main(args: Array<String>) {
	runApplication<DreamhubFrontApplication>(*args)
}
