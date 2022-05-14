package org.dmg.dreamhubserver.repository

import org.dmg.dreamhubserver.model.Setting
import org.springframework.data.repository.CrudRepository

interface SettingRepository: CrudRepository<Setting, Long>