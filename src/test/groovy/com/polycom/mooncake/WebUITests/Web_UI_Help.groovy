package com.polycom.mooncake.WebUITests

import com.polycom.honeycomb.moonCake.webui.page.DeviceStatusPages.DeviceStatusPage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared

/**
 * Created by Ryan Qi on 6/13/2019
 */
class Web_UI_Help extends MoonCakeUITestSpec{
    @Shared
    Logger logger = LoggerFactory.getLogger(Web_UI_Help.class)


    def "Help button test"() {
        when:"Open main page"
        DeviceStatusPage deviceStatusPage = browser.at DeviceStatusPage

        and:"Click help button"
        deviceStatusPage.getMainHeader().getHelpButton().click()

        and:"Get pop up url"
        Set<String> afterPopup = driver.getWindowHandles()
        driver.switchTo().window((String)afterPopup.toArray()[1])
        pauseTest(5)

        //Will update office site after dev update
        then:"Check popup url"
        driver.currentUrl!=null
    }
}
