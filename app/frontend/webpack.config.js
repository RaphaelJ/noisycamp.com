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

var buildPath = path.resolve(__dirname, '../../public/javascripts/');
var nodeModulesPath = path.resolve(__dirname, 'node_modules');

/**
 * Base configuration object for Webpack
 */
module.exports = {
    entry: [
        './main.ts'
    ],
    output: {
        path: buildPath,
        filename: 'bundle.js',
        sourceMapFilename: 'bundle.map',
        publicPath: '/assets/javascripts/'
    },
    devtool: 'source-map',
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
                }
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
    ]
};
