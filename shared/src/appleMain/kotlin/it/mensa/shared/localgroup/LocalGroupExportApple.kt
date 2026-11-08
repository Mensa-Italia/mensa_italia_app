package it.mensa.shared.localgroup

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create

/**
 * The bytes of [LocalGroupRepository.exportMembers] as `Data`, to write to a
 * temporary file named [LocalGroupExportFile.fileName] and share. From Swift:
 * `LocalGroupExportAppleKt.localGroupExportData(bytes: bytes)`.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun localGroupExportData(bytes: ByteArray): NSData =
    if (bytes.isEmpty()) NSData() else bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.convert()) }
