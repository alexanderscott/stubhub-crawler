Stubhub Crawler
===============

This repo contains a number of utilities for collecting data from the Stubhub API.  It runs forever, in ~6 second intervals (the free tier of the API allows 10 requests a minute), querying the api or loading events to query from the database.

The following can be run with `gradle [name]`

 - **crawler**: This is the primary means of gathering data.  It asks the database what events to query about, than queries stubhub repeatedly, storing updates to that event's listing in the database.
 - **finder**: does a simple event search given a string title of an event, and writes out a readable list of event ids and names to a file called found_events.txt that can be modified before running the creator
 - **creator**: reads the event ids from found_events, then asks stubhub for a summary of each event, the results of which are stored in the database (venue is added if the event is at a new venue) so that the crawler will start querying on the event the next time it reloads.

It includes a very primitive monitoring script, monitor.py, that sends an email when an error is logged.  There's a million ways I might go with monitoring; I'm considering a small flask app that shows logs and some simple stats.
