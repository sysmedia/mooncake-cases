package com.polycom.mooncake.WebUITests.AdminSettings

import com.polycom.honeycomb.moonCake.webui.page.AdminSettingsPages.UploadBackgroundPage
import com.polycom.honeycomb.moonCake.webui.page.AdminSettingsPages.UploadFailPage
import com.polycom.honeycomb.moonCake.webui.page.DeviceStatusPages.DeviceStatusPage
import com.polycom.mooncake.WebUITests.MoonCakeUITestSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared

/**
 * Created by Ryan Qi on 6/17/2019
 */
class Web_UI_System_Upload_Background_failure extends MoonCakeUITestSpec{
    @Shared
    Logger logger = LoggerFactory.getLogger(Web_UI_System_Upload_Background_failure.class)

    @Shared
    def falseFilePath = "src/test/resources/WebUI/Background/1920x1200.jpg"

    def setupSpec() {
        DeviceStatusPage deviceStatusPage = browser.at DeviceStatusPage

        deviceStatusPage.showAdminSettingsSubMenu()
        deviceStatusPage.clickAdminSettingsSubMenu("UploadBackground")
    }

    def "Upload wrong background page test"() {
        when:"Open background upload page"
        UploadBackgroundPage uploadBackgroundPage = browser.at UploadBackgroundPage
        uploadBackgroundPage.uploadFile(falseFilePath)
        uploadBackgroundPage.getUploadButton().click()

        and:"Check upload failure"
        UploadFailPage uploadFailPage = browser.at UploadFailPage
        uploadFailPage.getOkButton().click()

        then:"Check page back"
        browser.at UploadBackgroundPage
    }
}
