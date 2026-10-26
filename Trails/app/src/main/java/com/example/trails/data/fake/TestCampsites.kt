// Sample Alberta campsites in Banff, Jasper and Hinton-Grande Cache.
// OWNER: shared — anyone can add via a small PR reviewed by any teammate.
package com.example.trails.data.fake

import com.example.trails.data.model.Campsite
import com.example.trails.data.model.DateRange

object TestCampsites {
    const val BANFF_ID = "campsite-banff-tunnel"
    const val JASPER_ID = "campsite-jasper-whistlers"
    const val HINTON_ID = "campsite-hinton-wildhorse"

    val all = listOf(
        Campsite(
            id = BANFF_ID,
            ownerId = TestUsers.OWNER_ID,
            name = "Tunnel Mountain Site 14",
            description = "Treed site a short drive from the Banff townsite with views of Cascade Mountain.",
            locationName = "Banff, AB",
            latitude = 51.1856,
            longitude = -115.5476,
            maxOccupancy = 6,
            pricePerNight = 42.0,
            tags = listOf("Fire pit", "Power hookup", "Showers"),
            rules = "Quiet hours 11pm-7am. Store all food in your vehicle (bear country).",
            cancellationPolicy = "Full refund up to 7 days before arrival.",
            availability = listOf(DateRange(daysFromNow(30), daysFromNow(60))),
            ratingAverage = 4.5,
            ratingCount = 2,
            activeLotteryId = TestLotteries.LOTTERY_ID
        ),
        Campsite(
            id = JASPER_ID,
            ownerId = TestUsers.OWNER_ID,
            name = "Whistlers Riverside Site 7",
            description = "Quiet loop near the Miette River, minutes from Jasper and the SkyTram.",
            locationName = "Jasper, AB",
            latitude = 52.8466,
            longitude = -118.0796,
            maxOccupancy = 4,
            pricePerNight = 38.5,
            tags = listOf("Fire pit", "Riverside", "Dark sky"),
            rules = "Max two tents. Elk frequent the area; keep a 30 m distance.",
            cancellationPolicy = "50% refund up to 3 days before arrival.",
            availability = listOf(DateRange(daysFromNow(30), daysFromNow(75))),
            ratingAverage = 5.0,
            ratingCount = 1,
            activeLotteryId = TestLotteries.LOTTERY_ID
        ),
        Campsite(
            id = HINTON_ID,
            ownerId = TestUsers.OWNER_ID,
            name = "Wildhorse Lake Site 3",
            description = "Lakeside site between Hinton and Grande Cache, good for paddling and fishing.",
            locationName = "Hinton-Grande Cache, AB",
            latitude = 53.4710,
            longitude = -117.8070,
            maxOccupancy = 8,
            pricePerNight = 30.0,
            tags = listOf("Lakefront", "Boat launch", "Pet friendly"),
            rules = "Pets on leash at all times. Pack out all garbage.",
            cancellationPolicy = "Full refund up to 14 days before arrival.",
            availability = listOf(DateRange(daysFromNow(45), daysFromNow(90)))
        )
    )
}
