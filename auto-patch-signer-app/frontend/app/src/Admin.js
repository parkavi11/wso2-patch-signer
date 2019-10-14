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


class Admin extends React.Component {

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

    //send request to validation service with input parameters
    sendValidationRequest = (value) => {
        value.preventDefault();
        if (this.state.firstInput) {
            this.setState({firstInput: false});
        }

        if (this.state.validProductName && this.state.validStatus && this.state.validVersion &&
            (this.state.developer.length > 0) && (this.state.patchId.length === 4)) {
            this.setState({processState: true});
            this.setState({isProcessing: true});
            request
                .post(URL_VALIDATE_PATCH)
                .set('Content-Type', 'application/x-www-form-urlencoded')
                .send("version=" + this.state.carbonV.value + "&patchId=" + this.state.patchId + "&state=" +
                    this.state.status.value + "&product=" + this.state.productName + "&developedBy=" +
                    this.state.developer + "&productType=" + this.state.productType)
                .end((function (err, res) {
                    console.log('service request send');
                    console.log(res.text);
                    this.setState({requestShow: res.text});
                    this.setState({isProcessing: false});
                }).bind(this));
        }
    };

    //send revert request
    sendRevertRequest = (value) => {
        value.preventDefault();
        if (this.state.patchIdRevert.length === 4) {
            request
                .post(URL_REVERT_PATCH)
                .set('Content-Type', 'application/x-www-form-urlencoded')
                .send("version=" + this.state.carbonVRevert.value + "&patchId=" + this.state.patchIdRevert +
                    "&onlySVNRevert=" + this.state.checkedRevert)
                .end((function (err, res) {
                    console.log('revert request send');
                    console.log(res.text);
                    this.setState({requestShowRevert: res.text})
                }).bind(this));
        }
    };

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

    refreshPage = (value) => {
        value.preventDefault();
        window.location.reload();
    };

    changeVersion = (value) => {
        this.setState({validVersion: true});
        this.setState({carbonV: value});
    };
    changeVersionRevert = (value) => {
        this.setState({carbonVRevert: value});
    };
    changeStatus = (value) => {
        this.setState({validStatus: true});
        this.setState({status: value});
    };
    changeProduct = (value) => {
        this.setState({validProductName: true});
        this.setState({productName: (this.state.productName + '\n' + value.value)});
    };

    onChangePatchId(event) {
        this.setState({patchId: event.target.value});
    };

    onChangePatchIdRevert(event) {
        this.setState({patchIdRevert: event.target.value});
    };

    onChangeDeveloper(event) {
        this.setState({developer: event.target.value});
    };

    onChangeCheckBox = () => {
        this.setState({checked: !this.state.checked});
    };

    onChangeCheckBoxRevert = () => {
        this.setState({checkedRevert: !this.state.checkedRevert});
    };

    //new methods goes here
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


        const processState = this.state.processState;
        const processDisplay = processState ? (
            <p>Process started! Please wait... </p>
        ) : (
            <p>Please fill and click start button</p>
        );

        const processStateAddProduct = this.state.processStateAddProduct;
        let processDisplayAddProduct = 0;

        if (processStateAddProduct === 0) {
            processDisplayAddProduct = <p>Please fill and click save button</p>;
        } else if (processStateAddProduct === 1) {
            processDisplayAddProduct = <p>Adding product process started! Please wait... </p>;
        } else if (processStateAddProduct === 2) {
            processDisplayAddProduct = <p>{this.state.requestShowAddProduct.split("#").join("\n")}</p>;
        } else {
            processDisplayAddProduct = <p>Something went wrong in the front end</p>;
        }

        let reg = /^\w+([\.-]?\w+)*@\w+([\.-]?\w+)*(\.\w{2,3})+$/;
        const validEmail = (reg.test(this.state.developer));

        const validId = this.state.patchId.length === 4;
        const isEnabled = (this.state.validProductName && this.state.validStatus && this.state.validVersion &&
            validEmail && validId && !this.state.isProcessing) || (this.state.firstInput);
        const errMsg = (isEnabled || this.state.isProcessing) ? (<p></p>) : (
            <p style={errorStyle}>Whoops! There are some problems with your inputs </p>);
        (this.state.productType) = (this.state.checked) ? ("wum") : ("vanilla");

        //code added for new feature
        const isEnabledAddNew = (this.state.validNewType && this.state.validNewWUMSupported &&
            this.state.validNewCarbonState && (this.state.addNewProductName.length > 0) &&
            (this.state.addNewVersion.length > 0) && (this.state.addNewKernelVersion.length > 0) &&
            (this.state.addNewAbbreviation.length > 0) && !this.state.isProcessingAddProduct) ||
            (this.state.addNewFirstInput);
        const errMsgAddNew = (isEnabledAddNew || this.state.isProcessingAddProduct) ? (<p></p>) : (
            <p style={errorStyle}>Whoops! There are some problems with your inputs </p>);

        return (
            <div style={appStyle}>

                <Tabs>
                    <TabList>
                        <Tab>Patch and Update Sign</Tab>
                        <Tab>Patch and Update Revert</Tab>
                        <Tab>Add New Product</Tab>
                    </TabList>

                    <TabPanel>
                        <div align="center">
                            <br/>
                            <p style={titleStyle}>
                                {this.state.validId}Carbon Version
                                <div style={selectStyle}>
                                    <Select
                                        name="Carbon Version"
                                        onChange={this.changeVersion.bind(this)}
                                        options={[
                                            {value: 'wilkes', label: 'Wilkes'},
                                            {value: 'hamming', label: 'Hamming'},
                                            {value: 'turing', label: 'Turing'},
                                        ]}
                                    />
                                </div>
                            </p>

                            <p style={titleStyle}>Patch Id <i style={italicStyle}>( 4-Digit Number )</i></p>
                            <input type="text" placeholder="enter patch id" style={inputStyle}
                                   onChange={this.onChangePatchId.bind(this)}/>

                            <p style={titleStyle}>PMT State
                                <div style={selectStyle}>
                                    <Select
                                        name="Patch Id"
                                        onChange={this.changeStatus.bind(this)}
                                        options={[
                                            {value: '1', label: 'Released not in Public SVN'},
                                            {value: '2', label: 'Released not Automated'},
                                            {value: '3', label: 'Released'},
                                        ]}
                                    />
                                </div>
                            </p>

                            <p style={titleStyle}>PMT Product <i style={italicStyle}>(Add all the products)</i>
                                <div style={selectStyle}>

                                    <Request
                                        url={URL_FETCH_DATA}
                                        method='get'
                                        accept='json'>
                                        {
                                            ({error, result, loading}) => {
                                                if (loading) {
                                                    return <div align="center"><p align="center">loading wso2
                                                        products...</p></div>;
                                                } else {
                                                    // console.log(result.body);
                                                    return (<Select
                                                        name="Product"
                                                        onChange={this.changeProduct.bind(this)}
                                                        options={result.body}
                                                    />)

                                                }
                                            }
                                        }
                                    </Request>
                                </div>
                            </p>

                            <div>
                                <pre style={italicStyle}><i>Selected Products: <br/>{this.state.productName}</i></pre>
                            </div>

                            <div style={italicStyle}>
                                <input type="checkbox" onChange={this.onChangeCheckBox}
                                       defaultChecked={this.state.checked}/> Use latest WUM updated wso2 products for
                                this validation. (use {this.state.productType} product)
                            </div>

                            <p style={titleStyle}>Developer Email</p>
                            <input type="email" placeholder="enter developer's email" value={this.state.developer}
                                   style={inputStyle} onChange={this.onChangeDeveloper.bind(this)}/>
                        </div>

                        <div>
                            <div>{errMsg}</div>
                            <div>
                                <form onSubmit={this.sendValidationRequest}>
                                    <br/>
                                    <button disabled={!isEnabled} type="Submit" style={buttonStyle}>Start</button>
                                </form>
                                <form onSubmit={this.refreshPage}>
                                    <br/>
                                    <button type="Submit" style={resetButtonStyle}>Reset</button>
                                </form>
                            </div>
                        </div>

                        <div style={displayStyle}>{processDisplay}</div>

                        <div style={resultStyle}>
                            <pre>{this.state.requestShow.split("#").join("\n")}</pre>
                        </div>

                    </TabPanel>

                    <TabPanel>
                        <div align="center">
                            <div>
                                <br/>
                                <p style={titleStyle}>
                                    {this.state.validId}Carbon Version

                                    <div style={selectStyle}>
                                        <Select
                                            name="Carbon Version"
                                            onChange={this.changeVersionRevert.bind(this)}
                                            options={[
                                                {value: 'wilkes', label: 'Wilkes'},
                                                {value: 'hamming', label: 'Hamming'},
                                                {value: 'turing', label: 'Turing'},
                                            ]}
                                        />
                                    </div>
                                </p>
                            </div>

                            <div>
                                <p style={titleStyle}>Patch Id <i style={italicStyle}>( 4-Digit Number )</i></p>
                                <input type="text" placeholder="enter patch id" style={inputStyle}
                                       onChange={this.onChangePatchIdRevert.bind(this)}/>
                            </div>

                            <div style={italicStyle}>
                                <input type="checkbox" onChange={this.onChangeCheckBoxRevert}
                                       defaultChecked={this.state.checkedRevert}/> Only revert in SVN Repository. Do not
                                delete data in Databases.
                            </div>

                            <div>
                                <br/>
                                <form onSubmit={this.sendRevertRequest}>
                                    <br/>
                                    <button disabled={true} type="Submit" style={buttonStyleRevert}>Revert</button>
                                </form>
                                <form onSubmit={this.refreshPage}>
                                    <br/>
                                    <button type="Submit" style={resetButtonStyle}>Reset</button>
                                </form>
                            </div>

                            <div style={resultStyle}>
                                <pre>{this.state.requestShowRevert}</pre>
                            </div>
                            ​
                        </div>

                    </TabPanel>

                    <TabPanel>
                        <div align="center">

                            <div>
                                <p style={titleStyle}>Name</p>
                                <input type="text" placeholder="Enter Name" value={this.state.addNewProductName}
                                       style={inputStyle} onChange={this.onChangeAddNewProductName.bind(this)}/>
                            </div>

                            <div>
                                <p style={titleStyle}>Version</p>
                                <input type="text" placeholder="Enter Version" value={this.state.addNewVersion}
                                       style={inputStyle} onChange={this.onChangeAddNewVersion.bind(this)}/>
                            </div>

                            <div>
                                <p style={titleStyle}>
                                    {this.state.validId}Carbon Version
                                    <div style={selectStyle}>
                                        <Select
                                            name="Carbon Version"
                                            onChange={this.changeCarbonVersion.bind(this)}
                                            options={[
                                                {value: 'wilkes', label: 'Wilkes'},
                                                {value: 'hamming', label: 'Hamming'},
                                                {value: 'turing', label: 'Turing'},
                                            ]}
                                        />
                                    </div>
                                </p>
                            </div>

                            <div>
                                <p style={titleStyle}>Kernel Version</p>
                                <input type="text" placeholder="Enter Kernel Version"
                                       value={this.state.addNewKernelVersion}
                                       style={inputStyle} onChange={this.onChangeAddNewKernelVersion.bind(this)}/>
                            </div>

                            <div>
                                <p style={titleStyle}>Abbreviation</p>
                                <input type="text" placeholder="Enter Abbreviation"
                                       value={this.state.addNewAbbreviation}
                                       style={inputStyle} onChange={this.onChangeAddNewAbbreviation.bind(this)}/>
                            </div>

                            <div>
                                <p style={titleStyle}>WUM supported
                                    <div style={selectStyle}>
                                        <Select
                                            name="Patch Id"
                                            onChange={this.changeNewWUMSupported.bind(this)}
                                            options={[
                                                {value: '1', label: 'Yes'},
                                                {value: '0', label: 'No'},
                                            ]}
                                        />
                                    </div>
                                </p>
                            </div>

                            <div>
                                <p style={titleStyle}>Type
                                    <div style={selectStyle}>
                                        <Select
                                            name="Patch Id"
                                            onChange={this.changeNewType.bind(this)}
                                            options={[
                                                {value: '1', label: 'patch'},
                                                {value: '2', label: 'update'},
                                                {value: '3', label: 'patch and update'},
                                            ]}
                                        />
                                    </div>
                                </p>
                            </div>
                        </div>

                        <div>
                            <div>{errMsgAddNew}</div>
                            <div>
                                <form onSubmit={this.sendAddNewProductRequest}>
                                    <br/>
                                    <button disabled={!isEnabledAddNew} type="Submit" style={buttonStyle}>Save
                                    </button>
                                </form>
                                <form onSubmit={this.refreshPage}>
                                    <br/>
                                    <button type="Submit" style={resetButtonStyle}>Reset</button>
                                </form>
                            </div>
                        </div>

                        <div style={displayStyle}>{processDisplayAddProduct}</div>

                        <div style={resultStyle}>
                            <pre>{this.state.requestShowAddProduct.split("#").join("\n")}</pre>
                        </div>

                    </TabPanel>
                </Tabs>
            </div>
        )
    }
}

export default Admin;
