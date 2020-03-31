package com.devbuild.waveshare213v2

import com.pi4j.io.gpio.*
import com.pi4j.io.spi.SpiChannel
import com.pi4j.io.spi.SpiDevice
import com.pi4j.io.spi.SpiFactory
import java.awt.image.BufferedImage

object Waveshare213v2 {

    private val spiDev = SpiFactory.getInstance(
        SpiChannel.CS0,
        4000000,
        SpiDevice.DEFAULT_SPI_MODE
    )
    private val gpio = GpioFactory.getInstance()

    private val reset: GpioPinDigitalOutput
    private val dc: GpioPinDigitalOutput
    private val cs: GpioPinDigitalOutput
    private val busy: GpioPinDigitalInput

    val height: Int = 250
    val width: Int = 122
    val linewidth: Int = calculateLineWidth(width)

    private var fullDisplay = true

    private val lutFullUpdate: UByteArray = ubyteArrayOf(
        0x80u, 0x60u, 0x40u, 0x00u, 0x00u, 0x00u, 0x00u, //LUT0: BB:     VS 0 ~7
        0x10u, 0x60u, 0x20u, 0x00u, 0x00u, 0x00u, 0x00u, //LUT1: BW:     VS 0 ~7
        0x80u, 0x60u, 0x40u, 0x00u, 0x00u, 0x00u, 0x00u, //LUT2: WB:     VS 0 ~7
        0x10u, 0x60u, 0x20u, 0x00u, 0x00u, 0x00u, 0x00u, //LUT3: WW:     VS 0 ~7
        0x00u, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u, //LUT4: VCOM:   VS 0 ~7

        0x03u, 0x03u, 0x00u, 0x00u, 0x02u, // TP0 A~D RP0
        0x09u, 0x09u, 0x00u, 0x00u, 0x02u, // TP1 A~D RP1
        0x03u, 0x03u, 0x00u, 0x00u, 0x02u, // TP2 A~D RP2
        0x00u, 0x00u, 0x00u, 0x00u, 0x00u, // TP3 A~D RP3
        0x00u, 0x00u, 0x00u, 0x00u, 0x00u, // TP4 A~D RP4
        0x00u, 0x00u, 0x00u, 0x00u, 0x00u, // TP5 A~D RP5
        0x00u, 0x00u, 0x00u, 0x00u, 0x00u, // TP6 A~D RP6

        0x15u, 0x41u, 0xA8u, 0x32u, 0x30u, 0x0Au
    )

    private val lutPartialUpdate: UByteArray = ubyteArrayOf(
        0x00u, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u, //LUT0: BB:     VS 0 ~7
        0x80u, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u, //LUT1: BW:     VS 0 ~7
        0x40u, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u, //LUT2: WB:     VS 0 ~7
        0x00u, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u, //LUT3: WW:     VS 0 ~7
        0x00u, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u, 0x00u, //LUT4: VCOM:   VS 0 ~7

        0x0Au, 0x00u, 0x00u, 0x00u, 0x00u, // TP0 A~D RP0
        0x00u, 0x00u, 0x00u, 0x00u, 0x00u, // TP1 A~D RP1
        0x00u, 0x00u, 0x00u, 0x00u, 0x00u, // TP2 A~D RP2
        0x00u, 0x00u, 0x00u, 0x00u, 0x00u, // TP3 A~D RP3
        0x00u, 0x00u, 0x00u, 0x00u, 0x00u, // TP4 A~D RP4
        0x00u, 0x00u, 0x00u, 0x00u, 0x00u, // TP5 A~D RP5
        0x00u, 0x00u, 0x00u, 0x00u, 0x00u, // TP6 A~D RP6

        0x15u, 0x41u, 0xA8u, 0x32u, 0x30u, 0x0Au
    )

    init {
        reset = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "RST")
        reset.setShutdownOptions(true, PinState.LOW);
        dc = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_06, "DC")
        dc.setShutdownOptions(true, PinState.LOW);
        cs = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_10, "CS")
        busy = gpio.provisionDigitalInputPin(RaspiPin.GPIO_05, "BUSY")
    }

    fun fullUpdate() {
        hardwareReset()

        waitUntilIdle()
        sendCommand(0x12u) // soft reset
        waitUntilIdle()

        sendCommand(0x74u) // set analog block control
        sendData(0x54u)
        sendCommand(0x7Eu) // set digital block control
        sendData(0x3Bu)

        sendCommand(0x01u) // Driver output control
        sendData(0xF9u)
        sendData(0x00u)
        sendData(0x00u)

        sendCommand(0x11u) // data entry mode
        sendData(0x01u)

        sendCommand(0x44u) // set Ram-X address start//end position
        sendData(0x00u)
        sendData(0x0Fu) // 0x0C-->(15+1)*8=128

        sendCommand(0x45u) // set Ram-Y address start//end position
        sendData(0xF9u) // 0xF9-->(249+1)=250
        sendData(0x00u)
        sendData(0x00u)
        sendData(0x00u)

        sendCommand(0x3Cu) // BorderWavefrom
        sendData(0x03u)

        sendCommand(0x2Cu) // VCOM Voltage
        sendData(0x55u)

        sendCommand(0x03u)
        sendData(lutFullUpdate[70])

        sendCommand(0x04u)
        sendData(lutFullUpdate[71])
        sendData(lutFullUpdate[72])
        sendData(lutFullUpdate[73])

        sendCommand(0x3Au) // Dummy Line
        sendData(lutFullUpdate[74])
        sendCommand(0x3Bu) // Gate time
        sendData(lutFullUpdate[75])

        sendCommand(0x32u)
        for (x in 0 until 70) {
            sendData(lutFullUpdate[x])
        }

        sendCommand(0x4Eu) // set RAM x address count to 0
        sendData(0x00u)
        sendCommand(0x4Fu) // set RAM y address count to 0X127
        sendData(0xF9u)
        sendData(0x00u)
        waitUntilIdle()

        this.fullDisplay = true
    }

    fun partialUpdate() {
        hardwareReset()

        sendCommand(0x2Cu) // VCOM Voltage
        sendData(0x26u)

        waitUntilIdle()

        sendCommand(0x32u)
        for (x in 0 until 70) {
            sendData(lutPartialUpdate[x])
        }

        sendCommand(0x37u)
        sendData(0x00u)
        sendData(0x00u)
        sendData(0x00u)
        sendData(0x00u)
        sendData(0x40u)
        sendData(0x00u)
        sendData(0x00u)

        sendCommand(0x22u)
        sendData(0xC0u)
        sendCommand(0x20u)
        waitUntilIdle()

        sendCommand(0x3Cu) // BorderWavefrom
        sendData(0x01u)

        fullDisplay = false
    }

    fun display() {
        if (fullDisplay) turnOnFullDisplay()
        else turnOnPartDisplay()
    }

    private fun turnOnFullDisplay() {
        sendCommand(0x22u)
        sendData(0xC7u)
        sendCommand(0x20u)
        waitUntilIdle()
    }

    private fun turnOnPartDisplay() {
        sendCommand(0x22u)
        sendData(0x0Cu)
        sendCommand(0x20u)
        waitUntilIdle()
    }

    fun clear(color: UByte) {
        sendCommand(0x24u)
        for (j in 0 until height) {
            for (i in 0 until linewidth) {
                sendData(color)
            }
        }

        display()
    }

    fun printImage(image: BufferedImage) {
        if (image.width == this.width) {
            printImage(toByteBuff(image))
        } else {
            printImage(toByteBuffH(image))
        }
    }

    private fun printImage(image: UByteArray) {
        sendCommand(0x24u)
        for (j in 0 until height) {
            for (i in 0 until linewidth) {
                sendData(image[j * linewidth + i])
            }
        }

        display()
    }

    private fun toByteBuff(img: BufferedImage): UByteArray {
        val imgWidth = if (this.width > img.width) img.width else this.width
        val imgHeight = if (this.height > img.height) img.height else this.height

        val buf = UByteArray(imgHeight * this.linewidth)
        buf.fill(0xFFu, 0, buf.size)

        for (y in 0 until imgHeight) {
            for (x in 0 until imgWidth) {
                if (img.getRGB(x, y).toUInt() == 0xff000000u) {
                    val xx = imgWidth - x - 1
                    buf[(xx / 8) + y * this.linewidth] =
                        buf[(xx / 8) + y * this.linewidth] and (0x80u shr (xx % 8)).toUByte().inv()
                }
            }
        }

        return buf
    }

    private fun toByteBuffH(img: BufferedImage): UByteArray {
        val imgWidth = if (this.height > img.width) img.width else this.height
        val imgHeight = if (this.width > img.height) img.height else this.width

        val buf = UByteArray(this.height * this.linewidth)
        buf.fill(0xFFu, 0, buf.size)

        for (y in 0 until imgHeight) {
            for (x in 0 until imgWidth) {
                val xx = y
                val xy = this.height - x - 1
                if (img.getRGB(x, y).toUInt() == 0xff000000u) {
                    val xxy = imgWidth - xy - 1
                    buf[(xx / 8) + xxy * this.linewidth] =
                        buf[(xx / 8) + xxy * this.linewidth] and (0x80u shr (xx % 8)).toUByte().inv()
                }
            }
        }

        return buf
    }

    fun sleep() {
        sendCommand(0x22u) // POWER OFF
        sendData(0xC3u)
        sendCommand(0x20u)

        sendCommand(0x10u) // enter deep sleep
        sendData(0x01u)
        waitMs(100)
    }

    private fun hardwareReset() {
        reset.high()
        waitMs(200)
        reset.low()
        waitMs(200)
        reset.high()
        waitMs(200)
    }

    private fun sendCommand(data: UByte) {
        dc.low()
        cs.low()
        spiDev.write(byteArrayOf(data.toByte()), 0, 1)
        cs.high()
    }

    private fun sendData(data: UByte) {
        dc.high()
        cs.low()
        spiDev.write(byteArrayOf(data.toByte()), 0, 1)
        cs.high()
    }

    private fun waitUntilIdle() {
        while (busy.isHigh) {
            waitMs(100)
        }
    }

    private fun calculateLineWidth(width: Int) = if (width % 8 > 0) width / 8 + 1 else width / 8

    private fun waitMs(waitMs: Long) = Thread.sleep(waitMs)
}
