import React from 'react';
import Select from 'react-select';
import Request from 'react-http-request';
import {Tab, Tabs, TabList, TabPanel} from 'react-tabs';
import 'react-tabs/style/react-tabs.css';
import request from '../node_modules/superagent/superagent';

const configData = require('./resouses/config');
//read config files parameters
const URL_FETCH_DATA = configData.SERVER_URL_FETCH_DATA;
const URL_VALIDATE_PATCH = configData.SERVER_URL_VALIDATE_PATCH;
const URL_REVERT_PATCH = configData.SERVER_URL_REVERT_PATCH;
const URL_ADD_NEW_PRODUCT_PATCH = configData.SERVER_URL_ADD_NEW_PRODUCT_PATCH;

class AddProduct extends React.Component{

    constructor() {
        super();
        this.state = {
            carbonV: "",
            patchId: "",
            status: "",
            productName: "",
            developer: "",
            productType: "vanilla",
            errorMsg: "",
            requestShow: "",
            patchIdRevert: "",
            carbonVRevert: "",
            requestShowRevert: "",
            processState: false,
            validVersion: false,
            validVersionRevert: false, //use this
            validId: false,
            validIdRevert: false, //use this
            validStatus: false,
            validProductName: false,
            validDeveloper: false,
            isValid: false,
            checked: false,
            checkedRevert: false,
            isProcessing: false,
            firstInput: true,
            //new fields
            addNewProductName: "",
            addNewVersion: "",
            addNewCarbonVersion: "",
            addNewKernelVersion: "",
            addNewAbbreviation: "",
            addNewWUMSupported: "",
            addNewType: "",
            addNewFirstInput: true,
            validNewType: false,
            validNewWUMSupported: false,
            validNewCarbonState: false,
            requestShowAddProduct: "",
            processStateAddProduct: 0,
            isProcessingAddProduct: false,
        }
    }

    //add new product request
    sendAddNewProductRequest = (value) => {
        value.preventDefault();
        if (this.state.addNewFirstInput) {
            this.setState({addNewFirstInput: false});
        }

        if ((this.state.addNewProductName.length > 0) && (this.state.addNewVersion.length > 0) &&
            (this.state.addNewKernelVersion.length > 0) && (this.state.addNewAbbreviation.length > 0) &&
            this.state.validNewType && this.state.validNewWUMSupported && this.state.validNewCarbonState) {

            this.setState({processStateAddProduct: 1});
            this.setState({isProcessingAddProduct: true});

            request
                .post(URL_ADD_NEW_PRODUCT_PATCH)
                .set('Content-Type', 'application/x-www-form-urlencoded')
                .send("product=" + this.state.addNewProductName +
                    "&productVersion=" + this.state.addNewVersion +
                    "&carbonVersion=" + this.state.addNewCarbonVersion.value +
                    "&kernelVersion=" + this.state.addNewKernelVersion +
                    "&productAbbreviation=" + this.state.addNewAbbreviation +
                    "&WUMSupported=" + this.state.addNewWUMSupported.value +
                    "&type=" + this.state.addNewType.value)
                .end((function (err, res) {
                    console.log('service request send');
                    console.log(res.text);
                    this.setState({requestShowAddProduct: res.text});
                    this.setState({isProcessingAddProduct: false});
                    // let successMessage = this.state.requestShowAddProduct.split("#").join("\n");
                    // console.log(successMessage);
                    // if (successMessage.trim() === 'Adding product to DB Finished Successfully') {
                    //     this.clearAddProduct();
                    // }
                    this.setState({processStateAddProduct: 2}); //check this
                }).bind(this));
        }
    };

    //text fields
    onChangeAddNewProductName(event) {
        this.setState({addNewProductName: event.target.value});
    }

    onChangeAddNewVersion(event) {
        this.setState({addNewVersion: event.target.value});
    }

    onChangeAddNewKernelVersion(event) {
        this.setState({addNewKernelVersion: event.target.value});
    }

    onChangeAddNewAbbreviation(event) {
        this.setState({addNewAbbreviation: event.target.value});
    }

    //drop downs
    changeNewType = (value) => {
        this.setState({validNewType: true});
        this.setState({addNewType: value});
    };
    changeNewWUMSupported = (value) => {
        this.setState({validNewWUMSupported: true});
        this.setState({addNewWUMSupported: value});
    };

    changeCarbonVersion(value) {
        this.setState({validNewCarbonState: true});
        this.setState({addNewCarbonVersion: value});
    };

    render() {
        const appStyle = {
            textAlign: "center",
            align: "center"
        };
        const selectStyle = {
            align: "center",
            maxWidth: 365,
            fontSize: 16
        };
        const italicStyle = {
            fontFamily: "Helvetica",
            fontSize: 12,
            fontWeight: 100,
            marginTop: 10,
            color: "#666666"
        };
        const titleStyle = {
            fontFamily: "Helvetica",
            fontSize: 16,
            fontWeight: 500,
            marginBottom: 2,
            color: "#111"
        };
        const errorStyle = {
            fontFamily: "Helvetica",
            fontSize: 18,
            fontWeight: 400,
            color: "#aa1212"
        };
        const inputStyle = {
            fontFamily: "Helvetica",
            fontSize: 16,
            width: 350,
            height: 30,
            margin: 0
        };
        const resultStyle = {
            fontFamily: "Helvetica",
            fontSize: 12,
            fontWeight: 100,
            color: "#444"
        };
        const displayStyle = {
            fontFamily: "Helvetica",
            fontSize: 20,
            color: "#454545"
        };
        const buttonStyle = {
            fontFamily: "Helvetica",
            fontSize: 15,
            backgroundColor: "#007AFF",
            paddingTop: 5,
            paddingBottom: 5,
            width: 150,
            color: "#fff",
            fontWeight: 600,
            borderRadius: "0%",
            boxShadow: 0,
        };
        const buttonStyleRevert = {
            fontFamily: "Helvetica",
            fontSize: 15,
            backgroundColor: "#992222",
            paddingTop: 5,
            paddingBottom: 5,
            width: 150,
            color: "#fff",
            fontWeight: 600,
            borderRadius: "0%",
            boxShadow: 0,
        };
        const resetButtonStyle = {
            fontFamily: "Helvetica",
            fontSize: 15,
            backgroundColor: "#cfcfcf",
            paddingTop: 5,
            paddingBottom: 5,
            width: 150,
            color: "#000",
            fontWeight: 200,
            borderRadius: "0%",
            boxShadow: 0,
        };
    }
}