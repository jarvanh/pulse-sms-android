/*
 * Copyright (C) 2017 Luke Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.klinker.messenger.shared.util.xml

import android.content.Context
import android.content.res.XmlResourceParser
import android.text.Html
import android.text.Spanned

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException

import java.io.IOException
import java.util.ArrayList

import xyz.klinker.messenger.shared.R

/**
 * Helper for parsing the changelog.xml resource into a usable array of strings that can
 * be displayed in an adapter.
 */
object ChangelogParser {

    private val ns: String? = null

    /**
     * Parses the changelog.xml file and returns a string array of each item.
     *
     * @param context the current application context.
     * @return an array of spanned strings for formatting and displaying.
     */
    fun parse(context: Context): Array<Spanned>? = try {
        val parser = context.resources.getXml(R.xml.changelog)
        parser.next()
        parser.nextTag()
        readChangelog(parser)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readChangelog(parser: XmlPullParser): Array<Spanned> {
        val items = ArrayList<Spanned>()

        parser.require(XmlPullParser.START_TAG, ns, "changelog")
        while (parser.next() != XmlPullParser.END_TAG) {
            val name = parser.name
            if ("version" == name) {
                items.add(readVersion(parser))
            }
        }

        return items.toTypedArray()
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readVersion(parser: XmlPullParser): Spanned {
        val versionInfo = StringBuilder()
        parser.require(XmlPullParser.START_TAG, ns, "version")
        versionInfo.append(readVersionNumber(parser))

        while (parser.next() != XmlPullParser.END_TAG) {
            val name = parser.name
            if ("text" == name) {
                versionInfo.append(readVersionText(parser))
            }
        }

        return Html.fromHtml(versionInfo.toString())
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readVersionNumber(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "version")
        val versionNumber = parser.getAttributeValue(null, "name")
        val description = parser.getAttributeValue(null, "description")
        var version = "<b><u>Version $versionNumber:</b></u>"
        if (description != null) {
            version += " " + description
        }
        version += "<br/><br/>"
        return version
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readVersionText(parser: XmlPullParser): String {
        parser.require(XmlPullParser.START_TAG, ns, "text")
        val text = "\t&#8226 " + readText(parser) + "<br/>"
        parser.require(XmlPullParser.END_TAG, ns, "text")
        return text
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text
            parser.nextTag()
        }
        return result
    }

}