package com.polycom.mooncake.WebUITests.Contact

import com.polycom.honeycomb.moonCake.webui.page.ContactsPages.ContactsPage
import com.polycom.honeycomb.moonCake.webui.page.ContactsPages.DeleteAllContactsPage
import com.polycom.honeycomb.moonCake.webui.page.ContactsPages.EditContactPage
import com.polycom.honeycomb.moonCake.webui.page.DeviceStatusPages.DeviceStatusPage
import com.polycom.mooncake.WebUITests.MoonCakeUITestSpec
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared

/**
 * Created by Ryan Qi on 6/03/2019
 */

class Web_UI_Contact_Search extends MoonCakeUITestSpec {
    @Shared
    Logger logger = LoggerFactory.getLogger(Web_UI_Contact_Search.class)

    def contacts = ["user1":"123456","user2":"321567","tom":"122221","tomi":"199087","sam":"95123",
                    "tony":"1221","user3":"12111","Person1":"12333"]

    def setupSpec() {
        DeviceStatusPage deviceStatusPage = browser.at DeviceStatusPage
        ContactsPage contactsPage = deviceStatusPage.getSideBarMenu().switchContactsPage()
    }

    def "Search contact test"() {
        when: "Click the delete all button"
        ContactsPage contactsPage = browser.at ContactsPage
        contactsPage.getDeleteAllContacts().click()

        and: "Click ok button"
        DeleteAllContactsPage deleteAllContactPage = browser.at DeleteAllContactsPage
        deleteAllContactPage.getOkButton().click()

        then: "Check value"
        contactsPage.getContactItems().size() == 0

        when: "Input contact name and number"
        contacts.each{
            name,number->
                contactsPage = browser.at ContactsPage
                contactsPage.getAddContact().click()
                EditContactPage editContactPage = browser.at EditContactPage
                editContactPage.inputName(name)
                editContactPage.inputNumber(number)
                editContactPage.saveButton.click()
                pauseTest(5)
        }

        and: "Input search string"
        contactsPage.inputSearchText("user")
        contactsPage.getSearchButton().click()
        contactsPage = browser.at ContactsPage

        then:"Check value"
        contactsPage.getContactItems().size() == 3

        when:"Input search string"
        contactsPage.inputSearchText("tom")
        contactsPage.getSearchButton().click()
        contactsPage = browser.at ContactsPage

        then:"Check value"
        contactsPage.getContactItems().size() == 2

        when:"Input search string"
        contactsPage.inputSearchText("123")
        contactsPage.getSearchButton().click()
        contactsPage = browser.at ContactsPage

        then:"Check value"
        contactsPage.getContactItems().size() == 3

        cleanup:
        contactsPage = browser.at ContactsPage
        contactsPage.getDeleteAllContacts().click()
        deleteAllContactPage = browser.at DeleteAllContactsPage
        deleteAllContactPage.getOkButton().click()
    }
}
