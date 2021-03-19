/**
 * This package provides utilities for managing (reading & parsing) configuration
 * options.
 * 
 * The configuration model supported by this package is similar to the .ini
 * files used in Windows, that is:
 * - Configuration contains a flat list of global options (e.g. options outside
 * 		sections within .ini file).
 * - Configuration optionally contains one or more sections -- each of which
 * 		contains a flat list of options (e.g. specific sections within .ini file). 
 * 
 * Entry point for initializing configurations is {@link com.github.solf.extra2.config.Configuration}
 * 
 * Services that need configuration should accept {@link com.github.solf.extra2.config.FlatConfiguration}
 * and/or {@link com.github.solf.extra2.config.SectionedConfiguration}
 */
package com.github.solf.extra2.config;

