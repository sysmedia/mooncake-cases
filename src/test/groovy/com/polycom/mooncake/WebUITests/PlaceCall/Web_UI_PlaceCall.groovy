package com.polycom.mooncake.WebUITests.PlaceCall

import com.polycom.honeycomb.Dma
import com.polycom.honeycomb.GroupSeries
import com.polycom.honeycomb.moonCake.webui.page.DeviceStatusPages.DeviceStatusPage
import com.polycom.honeycomb.moonCake.webui.page.PlaceCallPages.ManualCallPage
import com.polycom.mooncake.WebUITests.MoonCakeUITestSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by Ryan Qi on 6/19/2019
 */
class Web_UI_PlaceCall extends MoonCakeUITestSpec {
    @Shared
    Logger logger = LoggerFactory.getLogger(Web_UI_PlaceCall.class)

    @Shared
    Dma dma

    @Shared
    GroupSeries groupSeries

    @Shared
    String sipUri

    @Shared
    String gs_sip_username

    @Shared
    String centralDomain = "sqa.org"

    @Shared
    String dialString

    def setupSpec() {
        groupSeries = testContext.bookSut(GroupSeries.class, keyword)
        groupSeries.init()
        groupSeries.setEncryption("no")

        //Enable mooncak/GS with SIP mode
        groupSeries.enableSIP()
        moonCake.enableSIP()
        moonCake.setEncryption("off")
        dma = testContext.bookSut(Dma.class, keyword)
        sipUri = generateDialString(moonCake).sipUri
        gs_sip_username = generateDialString(groupSeries).sipUri
        dialString = gs_sip_username + "@" + groupSeries.ip

        //Register mooncake/GS to DMA
        moonCake.registerSip("TCP", true, "", dma.ip, "", sipUri, "")
        groupSeries.registerSip(gs_sip_username, centralDomain, "", dma.ip)
    }

    def cleanupSpec() {
        testContext.releaseSut(groupSeries)
        testContext.releaseSut(dma)
        moonCake.init()
    }

    @Unroll
    def "Place P2P call with #callRate test"() {
        setup:
        moonCake.hangUp()
        groupSeries.hangUp()

        when: "Open place call page"
        DeviceStatusPage deviceStatusPage = browser.at DeviceStatusPage
        deviceStatusPage.showPlaceCallSubMenu()
        deviceStatusPage.clickPlaceCallSubMenu("ManualCall")

        and:"Select target call rate"
        ManualCallPage manualCallPage = browser.at ManualCallPage
        manualCallPage.getCallNumber().value(dialString)
        manualCallPage.getCallRate().selected = callRateSelect

        then:"Check value"
        manualCallPage.getCallRate().selectedText ==callRate

        when:"Place call"
        manualCallPage.getMakeCallButton().click()
        pauseTest(5)
        manualCallPage = browser.at ManualCallPage

        then: "Check hang up button is displaced"
        manualCallPage.getHangUpButton().isDisplayed()

        when: "Click hang up button"
        manualCallPage.getHangUpButton().click()
        manualCallPage = browser.at ManualCallPage

        then: "Check make call is displayed"
        manualCallPage.getMakeCallButton().isDisplayed()

        where:
        callRateSelect | callRate     | _
        "number:64"    | "Audio Only" | _
        "number:256"   | "256Kbps"    | _
        "number:384"   | "384Kbps"    | _
        "number:512"   | "512Kbps"    | _
        "number:768"   | "768Kbps"    | _
        "number:1024"  | "1024Kbps"   | _
        "number:1536"  | "1536Kbps"   | _
        "number:2048"  | "2048Kbps"   | _
        "number:3072"  | "3072Kbps"   | _
        "number:4096"  | "4096Kbps"   | _

    }
}
