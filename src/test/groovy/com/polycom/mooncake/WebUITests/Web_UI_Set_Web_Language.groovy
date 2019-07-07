package com.polycom.mooncake.WebUITests

import com.polycom.honeycomb.moonCake.webui.page.DeviceStatusPages.DeviceStatusPage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared

/**
 * Created by Ryan Qi on 6/14/2019
 */
class Web_UI_Set_Web_Language extends MoonCakeUITestSpec{
    @Shared
    Logger logger = LoggerFactory.getLogger(Web_UI_Set_Web_Language.class)

    def "Help button test"() {
        when: "Open main page"
        DeviceStatusPage deviceStatusPage = browser.at DeviceStatusPage
        deviceStatusPage.getMainHeader().getLanguageList().click()

        and:"Switch to Chinese"
        deviceStatusPage.getMainHeader().getChinese().click()

        then:"Check value"
        deviceStatusPage.getMainHeader().getLanguageList().text() == "语言"

        when:"Switch to English"
        deviceStatusPage.getMainHeader().getEnglish().click()

        then:"Check value"
        deviceStatusPage.getMainHeader().getLanguageList().text() == "Language"
    }
}
