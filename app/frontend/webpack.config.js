/* Noisycamp is a platform for booking music studios.
 * Copyright (C) 2019  Raphael Javaux <raphaeljavaux@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

/* global __dirname */
var webpack = require('webpack');
var path = require('path');

var VueLoaderPlugin = require('vue-loader/lib/plugin')

var buildPath = path.resolve(__dirname, '../../public/javascripts/app');
var nodeModulesPath = path.resolve(__dirname, 'node_modules');

module.exports = (env, argv) => {
    return {
        devtool: argv.mode === 'production' ? 'source-map' : 'eval',
        entry: {
            main: './entry_points/main.ts',
            index: {
                import: './entry_points/index.ts',
                dependOn: 'main',
            },

            studios_index: {
                import: './entry_points/studios/index.ts',
                filename: 'studios/index.js',
                dependOn: 'main',
            },
            studios_show: {
                import: './entry_points/studios/show.ts',
                filename: 'studios/show.js',
                dependOn: 'main',
            },
            studios_embedded: {
                import: './entry_points/studios/embedded.ts',
                filename: 'studios/embedded.js',
                dependOn: 'main',
            },

            studios_booking_review: {
                import: './entry_points/studios/booking/review.ts',
                filename: 'studios/booking/review.js',
                dependOn: 'main',
            },

            stripe_checkout_redirect: {
                import: './entry_points/stripe_checkout_redirect.ts',
                dependOn: 'main',
            },

            account_studios_create: {
                import: './entry_points/account/studios/create.ts',
                filename: 'account/studios/create.js',
                dependOn: 'main',
            },
            account_studios_settings: {
                import: './entry_points/account/studios/settings.ts',
                filename: 'account/studios/settings.js',
                dependOn: 'main',
            },

            account_studios_bookings_calendar: {
                import: './entry_points/account/studios/bookings/calendar.ts',
                filename: 'account/studios/bookings/calendar.js',
                dependOn: 'main',
            },
            account_studios_bookings_create: {
                import: './entry_points/account/studios/bookings/create.ts',
                filename: 'account/studios/bookings/create.js',
                dependOn: 'main',
            },
        },
        output: {
            path: buildPath,
            filename: '[name].js',
            sourceMapFilename: '[name].map',
            publicPath: '/assets/javascripts/app',
            library: 'NoisyCamp',
        },
        externals: {
        },
        module: {
            rules: [
                {
                    test: /\.css$/,
                    use: ['style-loader','css-loader']
                },
                {
                    test: /\.less$/,
                    use: ['style-loader', 'css-loader', 'less-loader']
                },
                {
                    test: /\.ts$/,
                    loader: 'ts-loader',
                    options: {
                        appendTsSuffixTo: [/\.vue$/]
                    },
                },
                {
                    test: /\.(jpg|png)$/,
                    use: 'url-loader?limit=100000'
                },
                {
                    test: /\.svg$/,
                    use: 'url-loader?limit=10000&mimetype=image/svg+xml'
                },
                {
                    test: /\.vue$/,
                    loader: 'vue-loader',
                    options: {}
                }
            ]
        },
        resolve: {
            extensions: ['.ts','.js','.json','.css','.html'],
            alias: {
                'vue$': 'vue/dist/vue.esm.js'
            }
        },
        plugins: [
            new webpack.ContextReplacementPlugin(
                path.resolve(__dirname, '.')
            ),
            new webpack.HotModuleReplacementPlugin(),
            new VueLoaderPlugin(),
        ],
    }
};
