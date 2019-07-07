package com.polycom.mooncake.WebUITests.AdminSettings

import com.polycom.honeycomb.moonCake.webui.page.AdminSettingsPages.UploadBackgroundPage
import com.polycom.honeycomb.moonCake.webui.page.DeviceStatusPages.DeviceStatusPage
import com.polycom.mooncake.WebUITests.MoonCakeUITestSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared

/**
 * Created by Ryan Qi on 6/17/2019
 */
class Web_UI_System_Upload_Background extends MoonCakeUITestSpec {
    @Shared
    Logger logger = LoggerFactory.getLogger(Web_UI_System_Upload_Background.class)

    @Shared
    def correctFilePath = "src/test/resources/WebUI/Background/1920x1080.bmp"

    def setupSpec() {
        DeviceStatusPage deviceStatusPage = browser.at DeviceStatusPage

        deviceStatusPage.showAdminSettingsSubMenu()
        deviceStatusPage.clickAdminSettingsSubMenu("UploadBackground")
    }

    def "Upload correct background page test"() {
        when: "Open background upload page"
        UploadBackgroundPage uploadBackgroundPage = browser.at UploadBackgroundPage

        and: "Upload correct background file"
        uploadBackgroundPage.uploadFile(correctFilePath)
        uploadBackgroundPage.getUploadButton().click()
        pauseTest(10)

        then: "Check page"
        browser.at UploadBackgroundPage
    }
}
