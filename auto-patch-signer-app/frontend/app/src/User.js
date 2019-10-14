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


class User extends React.Component {

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
            // patchIdRevert: "",
            // carbonVRevert: "",
            // requestShowRevert: "",
            processState: false,
            validVersion: false,
            // validVersionRevert: false, //use this
            validId: false,
            // validIdRevert: false, //use this
            validStatus: false,
            validProductName: false,
            validDeveloper: false,
            isValid: false,
            checked: false,
            // checkedRevert: false,
            isProcessing: false,
            firstInput: true,
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
        // const buttonStyleRevert = {
        //     fontFamily: "Helvetica",
        //     fontSize: 15,
        //     backgroundColor: "#992222",
        //     paddingTop: 5,
        //     paddingBottom: 5,
        //     width: 150,
        //     color: "#fff",
        //     fontWeight: 600,
        //     borderRadius: "0%",
        //     boxShadow: 0,
        // };
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

        let reg = /^\w+([\.-]?\w+)*@\w+([\.-]?\w+)*(\.\w{2,3})+$/;
        const validEmail = (reg.test(this.state.developer));

        const validId = this.state.patchId.length === 4;
        const isEnabled = (this.state.validProductName && this.state.validStatus && this.state.validVersion &&
            validEmail && validId && !this.state.isProcessing) || (this.state.firstInput);
        const errMsg = (isEnabled || this.state.isProcessing) ? (<p></p>) : (
            <p style={errorStyle}>Whoops! There are some problems with your inputs </p>);
        (this.state.productType) = (this.state.checked) ? ("wum") : ("vanilla");


        return (
            <div style={appStyle}>

                <Tabs>
                    <TabList>
                        <Tab>Patch and Update Sign</Tab>
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

                </Tabs>
            </div>
        )
    }
}

export default User;

