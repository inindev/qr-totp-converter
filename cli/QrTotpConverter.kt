//
// Copyright 2025, John Clark <inindev@gmail.com>. All rights reserved.
// Licensed under the Apache License, Version 2.0. See LICENSE file in the project root for full license information.
//
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.EncodeHintType
import com.google.zxing.LuminanceSource
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.stream.FileImageOutputStream
import kotlin.io.path.Path
import kotlin.io.path.writeText

private const val QR_SIZE = 300
private const val BLACK = 0x000000
private const val WHITE = 0xFFFFFF
private const val JPG_QUALITY = 0.5f

fun main(args: Array<String>) {
    if (args.size < 2) return println("Usage: qr-totp-converter <encode|decode> [-o output] <input>")

    val mode = args[0]
    val (input, outputFile) = parseArgs(args.drop(1)) ?: return println("Error: Input is required")

    when (mode) {
        "encode" -> encodeToQrCode(input, outputFile ?: "qrcode.png")
            .also { println("QR code written to $it") }
        "decode" -> decodeQrCode(input).let { url ->
            outputFile?.let { Path(it).writeText(url).also { println("TOTP URL written to $it") } } ?: println(url)
        }
        else -> println("Error: Mode must be 'encode' or 'decode'")
    }
}

private fun encodeToQrCode(totpUrl: String, outputFile: String): String {
    val bitMatrix = QRCodeWriter().encode(
        totpUrl, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE, mapOf(EncodeHintType.CHARACTER_SET to "UTF-8")
    )
    val image = BufferedImage(QR_SIZE, QR_SIZE, BufferedImage.TYPE_BYTE_BINARY).apply {
        for (x in 0 until QR_SIZE) {
            for (y in 0 until QR_SIZE) {
                setRGB(x, y, if (bitMatrix[x, y]) BLACK else WHITE)
            }
        }
    }

    val (finalOutput, format) = when {
        outputFile.endsWith(".jpg", ignoreCase = true) -> outputFile to "jpg"
        outputFile.endsWith(".png", ignoreCase = true) -> outputFile to "png"
        outputFile.endsWith(".gif", ignoreCase = true) -> outputFile to "gif"
        outputFile.endsWith(".webp", ignoreCase = true) -> outputFile to "webp"
        else -> "$outputFile.png" to "png"
    }
    val file = Path(finalOutput).toFile()

    when (format) {
        "jpg" -> {
            val writer = ImageIO.getImageWritersByFormatName("jpg").next()
            val param = writer.defaultWriteParam.apply {
                compressionMode = ImageWriteParam.MODE_EXPLICIT
                compressionQuality = JPG_QUALITY
            }
            writer.output = FileImageOutputStream(file)
            writer.write(null, javax.imageio.IIOImage(image, null, null), param)
            writer.dispose()
        }
        "webp" -> {
            System.err.println("Error: WEBP format requires additional library (e.g., imageio-webp). Falling back to PNG.")
            ImageIO.write(image, "png", file)
        }
        else -> ImageIO.write(image, format, file) // png or gif
    }
    return finalOutput
}

private fun decodeQrCode(inputFile: String): String {
    val file = Path(inputFile).toFile()
    if (!file.exists() || !file.canRead()) {
        throw IllegalArgumentException("Cannot read file: $inputFile (exists: ${file.exists()}, readable: ${file.canRead()})")
    }
    val image = ImageIO.read(file) ?: throw IllegalArgumentException("Failed to load image: $inputFile")
    return QRCodeReader().decode(BinaryBitmap(HybridBinarizer(SimpleLuminanceSource(image)))).text
}

private fun parseArgs(args: List<String>): Pair<String, String?>? {
    var input: String? = null
    var output: String? = null

    args.forEachIndexed { i, arg ->
        when {
            arg == "-o" && i + 1 < args.size -> output = args[i + 1]
            arg != "-o" && (i == 0 || args[i - 1] != "-o") -> input = arg
        }
    }

    return input?.let { it to output }
}

class SimpleLuminanceSource(private val image: BufferedImage) : LuminanceSource(image.width, image.height) {
    private val luminances = ByteArray(width * height) { index ->
        val (x, y) = index % width to index / width
        val pixel = image.getRGB(x, y)
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        ((r * 77 + g * 150 + b * 28) shr 8).toByte()
    }

    override fun getRow(y: Int, row: ByteArray?): ByteArray =
        (row?.takeIf { it.size >= width } ?: ByteArray(width)).also {
            System.arraycopy(luminances, y * width, it, 0, width)
        }

    override fun getMatrix(): ByteArray = luminances
}
