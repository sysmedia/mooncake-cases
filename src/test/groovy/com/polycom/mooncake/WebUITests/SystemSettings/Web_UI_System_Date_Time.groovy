package com.polycom.mooncake.WebUITests.SystemSettings

import com.polycom.honeycomb.moonCake.webui.page.DeviceStatusPages.DeviceStatusPage
import com.polycom.honeycomb.moonCake.webui.page.SystemSettingsPages.DateAndTimePage
import com.polycom.mooncake.WebUITests.MoonCakeUITestSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by Ryan Qi on 6/03/2019
 */

class Web_UI_System_Date_Time extends MoonCakeUITestSpec {
    @Shared
    Logger logger = LoggerFactory.getLogger(Web_UI_System_Date_Time.class)

    def setupSpec() {
        DeviceStatusPage deviceStatusPage = browser.at DeviceStatusPage

        deviceStatusPage.showSystemSettingsSubMenu()
        deviceStatusPage.clickSystemSettingsSubMenu("DateandTime")
    }

    @Unroll
    def "Test Time Server #mode on WebUI"() {
        when: "Open Date and Time Page"
        DateAndTimePage dateAndTimePage = browser.at DateAndTimePage

        and: "Set Time Server mode"
        dateAndTimePage.getTimeServer().selected = "string:" + mode

        then: "Check value"
        dateAndTimePage.getTimeServer().selectedText == modeOnWeb

        where:
        mode     | modeOnWeb | _
        "manual" | "Manual"  | _
        "auto"   | "Auto"    | _
    }

    def "Test Date Format #mode on WebUI"() {
        when: "Open Date and Time Page"
        DateAndTimePage dateAndTimePage = browser.at DateAndTimePage

        and: "Set Date Format mode"
        dateAndTimePage.getDateFormat().selected = "number:" + mode

        then: "Check value"
        dateAndTimePage.getDateFormat().selectedText == modeOnWeb

        where:
        mode | modeOnWeb    | _
        "0"  | "MM-dd-yyyy" | _
        "1"  | "dd-MM-yyyy" | _
        "2"  | "yyyy-MM-dd" | _

    }

    def "Test Time Format #mode on WebUI"() {
        when: "Open Date and Time Page"
        DateAndTimePage dateAndTimePage = browser.at DateAndTimePage

        and: "Set Time Format mode"
        dateAndTimePage.getTimeFormat().selected = "number:" + mode

        then: "Check value"
        dateAndTimePage.getTimeFormat().selectedText == modeOnWeb

        where:
        mode | modeOnWeb       | _
        "0"  | "12-hour Clock" | _
        "1"  | "24-hour Clock" | _
    }
}
