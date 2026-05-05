import { NextRequest, NextResponse } from "next/server";
import axios from "axios";

const API_URL = process.env.NEXT_PUBLIC_API_URL?.replace(/\/$/, "");

function getTargetUrl(request: NextRequest) {
    const url = new URL(request.url);
    const proxyPrefix = "/api/proxy/";
    const pathname = url.pathname;

    // Remove o prefixo /api/proxy/
    const endpointPath = pathname.startsWith(proxyPrefix)
        ? pathname.slice(proxyPrefix.length)
        : pathname;

    return `${API_URL}/${endpointPath}${url.search}`;
}

export async function GET(request: NextRequest) {
    try {
        const targetUrl = getTargetUrl(request);

        const axiosResponse = await axios.request({
            url: targetUrl,
            method: request.method,
        });

        return NextResponse.json(axiosResponse.data);
    } catch (error: any) {
        if (error.response) {
            return NextResponse.json(
                error.response.data,
                { status: error.response.status }
            );
        }
        console.error("Proxy GET error:", {
            message: error.message,
            code: error.code,
            url: request.url,
            targetUrl: getTargetUrl(request),
            response: error?.response?.data,
        });

        return NextResponse.json(
            { error: "Erro ao comunicar com a API externa", details: error.message },
            { status: 500 }
        );
    }
}

export async function POST(request: NextRequest) {
    return handleProxy(request);
}

export async function PUT(request: NextRequest) {
    return handleProxy(request);
}

export async function DELETE(request: NextRequest) {
    try {
        const targetUrl = getTargetUrl(request);

        const axiosResponse = await axios.request({
            url: targetUrl,
            method: request.method,
        });

        return NextResponse.json(axiosResponse.data);
    } catch (error: any) {
        console.error("Proxy DELETE error:", {
            message: error.message,
            code: error.code,
            url: request.url,
            targetUrl: getTargetUrl(request),
            response: error?.response?.data,
        });

        return NextResponse.json(
            { error: "Erro ao comunicar com a API externa", details: error.message },
            { status: 500 }
        );
    }
}

async function handleProxy(request: NextRequest) {
    const targetUrl = getTargetUrl(request);

    try {
        const contentType = request.headers.get("content-type");

        let axiosResponse;

        if (contentType && contentType.includes("multipart/form-data")) {
            const formData = await request.formData();
            const headers = Object.fromEntries(request.headers.entries());
            delete headers['content-type'];

            axiosResponse = await axios.request({
                url: targetUrl,
                method: request.method,
                data: formData,
                headers: {
                    ...headers,
                    'Accept': 'application/json',
                },
            });
        } else {
            const body = await request.json();

            const axiosResult = await axios.request({
                url: targetUrl,
                method: request.method,
                data: body,
                headers: {
                    'Content-Type': 'application/json',
                },
            });

            return NextResponse.json(axiosResult.data);
        }
        return NextResponse.json(axiosResponse.data);

    } catch (error: any) {
        if (error.response) {
            return NextResponse.json(
                error.response.data,
                { status: error.response.status }
            );
        }

        console.error("Proxy error:", {
            message: error.message,
            code: error.code,
            url: request.url,
            targetUrl,
            response: error?.response?.data,
        });
        return NextResponse.json(
            { error: "Erro ao comunicar com a API externa", details: error.message },
            { status: 500 }
        );
    }
}

