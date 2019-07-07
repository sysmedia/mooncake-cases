package com.polycom.mooncake.WebUITests.AdminSettings

import com.polycom.honeycomb.moonCake.webui.page.AdminSettingsPages.RestartConfirmPage
import com.polycom.honeycomb.moonCake.webui.page.AdminSettingsPages.SystemRestartPage
import com.polycom.honeycomb.moonCake.webui.page.DeviceStatusPages.DeviceStatusPage
import com.polycom.mooncake.WebUITests.MoonCakeUITestSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared

/**
 * Created by Ryan Qi on 6/14/2019
 */
class Web_UI_System_Reboot extends MoonCakeUITestSpec{
    @Shared
    Logger logger = LoggerFactory.getLogger(Web_UI_System_Reboot.class)

    def setupSpec() {
        DeviceStatusPage deviceStatusPage = browser.at DeviceStatusPage

        deviceStatusPage.showAdminSettingsSubMenu()
        deviceStatusPage.clickAdminSettingsSubMenu("SystemRestart")
    }

    def "System reboot test"(){
        when:"Open system restart page"
        SystemRestartPage systemRestartPage = browser.at SystemRestartPage

        and:"Click restart button"
        systemRestartPage.getRestartButton().click()

        then:"Check restart confirm page"
        browser.at RestartConfirmPage

        when:"Click cancel button"
        RestartConfirmPage restartConfirmPage = browser.at RestartConfirmPage
        restartConfirmPage.getCancelButton().click()

        then:"Check current page"
        browser.at SystemRestartPage
    }
}
