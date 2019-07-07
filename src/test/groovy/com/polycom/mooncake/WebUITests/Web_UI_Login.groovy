package com.polycom.mooncake.WebUITests

import com.polycom.honeycomb.moonCake.webui.page.AdminSettingsPages.PasswordPage
import com.polycom.honeycomb.moonCake.webui.page.DeviceStatusPages.DeviceStatusPage
import com.polycom.honeycomb.moonCake.webui.page.LoginPage
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared


/**
 * Created by Ryan Qi on 6/14/2019
 */
class Web_UI_Login extends MoonCakeUITestSpec {
    @Shared
    Logger logger = LoggerFactory.getLogger(Web_UI_Login.class)

    @Shared
    def username = "admin"

    @Shared
    def password = "Polycom123"

    def setupSpec() {
        DeviceStatusPage deviceStatusPage = browser.at DeviceStatusPage

        deviceStatusPage.showAdminSettingsSubMenu()
        deviceStatusPage.clickAdminSettingsSubMenu("Password")
    }

    def "Login page test"() {
        when: "Open password setting page for setting password"
        PasswordPage passwordPage = browser.at PasswordPage

        and: "change the admin password"
        passwordPage.changePassword("", password, password)
        pauseTest(3)

        then: "change success, page go to login page"
        browser.at LoginPage

        when: "Change language to Chinese from login page"
        LoginPage loginPage = browser.at LoginPage
        loginPage.getChineseButton().click()

        and: "Login system"
        loginPage.getUserName().value(username)
        loginPage.getPassword().value(password)
        loginPage.getSubmitButton().click()

        and: "Located in home page"
        DeviceStatusPage deviceStatusPage = browser.at DeviceStatusPage

        then: "Check language changed"
        deviceStatusPage.getMainHeader().getLanguageList().text() == "语言"

        when: "Restore language to english"
        deviceStatusPage.getMainHeader().getLanguageList().click()
        deviceStatusPage.getMainHeader().getEnglish().click()

        then: "Change language changed"
        deviceStatusPage.getMainHeader().getLanguageList().text() == "Language"

        when: "Open password setting page"
        deviceStatusPage.showAdminSettingsSubMenu()
        deviceStatusPage.clickAdminSettingsSubMenu("Password")

        and: "Restore password back"
        passwordPage = browser.at PasswordPage
        passwordPage.getOldPassword().value(password)
        passwordPage.getNewPassword().value("")
        passwordPage.getConfirmPassword().value("")
        passwordPage.getSubmitButton().click()

        then: "Return to main page"
        browser.at DeviceStatusPage
    }

}
