import React from 'react';
import Request from 'react-http-request';
import 'react-tabs/style/react-tabs.css';
import request from '../node_modules/superagent/superagent';
import User from './User';
import Admin from './Admin';
import ReactDOM from "react-dom";

const configData = require('./resouses/config');
//read config files parameters
const URL_FETCH_ADMIN_ROLE = configData.SERVER_URL_FETCH_ADMIN_ROLE;

class App extends React.Component {

    constructor() {
        super();
        this.state = {}
    }

    refreshPage = (value) => {
        value.preventDefault();
        window.location.reload();
    };

    render() {
        const appStyle = {
            textAlign: "center",
            align: "center"
        };
        const headTitleStyle = {
            fontFamily: "Helvetica",
            textAlign: "center",
            color: "#444",
            backgroundColor: "#efefef",
            height: 70,
        };
        const logoStyle = {
            height: 22,
            width: 50,
            marginTop: 10
        };
        const mainTitleStyle = {
            fontFamily: "Helvetica",
            fontSize: 16,
            fontWeight: 100,
            margin: 0
        };

        return (
            <div style={appStyle}>
                <div style={headTitleStyle}>
                    <img src={process.env.PUBLIC_URL + '/assets/images/WSO2-Software-Logo.png'} style={logoStyle}/>
                    <p style={mainTitleStyle}>Product Patch and Update Signer </p>
                    <br/>
                </div>

                <Request url={URL_FETCH_ADMIN_ROLE}
                         method='get'
                         accept='json'>
                    {
                        ({error, result, loading}) => {
                            if (loading) {
                                return (<h4>Please wait, Tabs are loading...</h4>);
                            } else {
                                // console.log(result.body);
                                if (result.body === 1) {
                                    return (<Admin/>);
                                }else{
                                    return (<User/>);
                                }
                            }
                        }
                    }
                </Request>
            </div>
        )
    }
}

export default App;
