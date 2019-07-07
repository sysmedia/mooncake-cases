package com.polycom.mooncake.WebUITests.SystemSettings

import com.polycom.honeycomb.moonCake.webui.page.DeviceStatusPages.DeviceStatusPage
import com.polycom.honeycomb.moonCake.webui.page.SystemSettingsPages.GeneralPage
import com.polycom.mooncake.WebUITests.MoonCakeUITestSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared
import spock.lang.Unroll

/**
 * Created by qxu on 5/22/2019
 */
class Web_UI_System_Set_Language extends MoonCakeUITestSpec{
    @Shared
    Logger logger = LoggerFactory.getLogger(Web_UI_System_Set_Language.class)

    def setupSpec() {
        DeviceStatusPage deviceStatusPage = browser.at DeviceStatusPage

        deviceStatusPage.showSystemSettingsSubMenu()
        deviceStatusPage.clickSystemSettingsSubMenu("General")
    }

    @Unroll
    def "Set Local Interface Language to #languageOnWeb on WebUI"(){
        when: "Open General page"
        GeneralPage generalPage = browser.at GeneralPage

        and: "Set local interface language"
        generalPage.getLocalInterfaceLanguage().selected = "number:" + language
        pauseTest(2)

        then: "check language"
        generalPage.getLocalInterfaceLanguage().selectedText == languageOnWeb

        where:
        language  | languageOnWeb          | _
        "0"       |  "English US"          | _
        "1"       |  "Chinese Simplified"  | _
    }

}
