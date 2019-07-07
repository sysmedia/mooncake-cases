package com.polycom.mooncake.WebUITests.AdminSettings

import com.polycom.honeycomb.moonCake.webui.page.AdminSettingsPages.FactoryResetConfirmPage
import com.polycom.honeycomb.moonCake.webui.page.AdminSettingsPages.FactoryResetPage
import com.polycom.honeycomb.moonCake.webui.page.DeviceStatusPages.DeviceStatusPage
import com.polycom.mooncake.WebUITests.MoonCakeUITestSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared

/**
 * Created by Ryan Qi on 6/14/2019
 */
class Web_UI_Security_Factory_Restore extends MoonCakeUITestSpec{
    @Shared
    Logger logger = LoggerFactory.getLogger(Web_UI_Security_Factory_Restore.class)

    def setupSpec() {
        DeviceStatusPage deviceStatusPage = browser.at DeviceStatusPage

        deviceStatusPage.showAdminSettingsSubMenu()
        deviceStatusPage.clickAdminSettingsSubMenu("FactoryReset")
    }

    def "verify MoonCake Web UI password reset function"() {
        when: "open password page"
        FactoryResetPage factoryResetPage = browser.at FactoryResetPage

        and: "Click restore button"
        factoryResetPage.getResetButton().click()

        then:"Check pop up window"
        browser.at FactoryResetConfirmPage

        when: "Click cancel button"
        FactoryResetConfirmPage factoryResetConfirmPage = browser.at FactoryResetConfirmPage
        factoryResetConfirmPage.getCancelButton().click()

        then:"Check page should turn back"
        browser.at FactoryResetPage
    }
}
