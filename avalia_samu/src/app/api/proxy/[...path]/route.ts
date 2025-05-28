import { NextRequest, NextResponse } from "next/server";
import axios from "axios";

const API_URL = process.env.NEXT_PUBLIC_API_URL;

export async function GET(request: NextRequest) {
    const { pathname, search } = new URL(request.url);

    // Extração do path após "/api/proxy/"
    const proxyPrefix = "/api/proxy/";
    const endpointPath = pathname.startsWith(proxyPrefix) ? pathname.slice(proxyPrefix.length) : "";
    const targetUrl = `${API_URL}/${endpointPath}${search}`;

    const axiosResponse = await axios.request({
        url: targetUrl,
        method: request.method,
    });

    return NextResponse.json(axiosResponse.data);
}

export async function POST(request: NextRequest) {
    return handleProxy(request);
}

export async function PUT(request: NextRequest) {
    return handleProxy(request);
}

export async function DELETE(request: NextRequest) {
    const { pathname, search } = new URL(request.url);

    // Extração do path após "/api/proxy/"
    const proxyPrefix = "/api/proxy/";
    const endpointPath = pathname.startsWith(proxyPrefix) ? pathname.slice(proxyPrefix.length) : "";
    const targetUrl = `${API_URL}/${endpointPath}${search}`;

    const axiosResponse = await axios.request({
        url: targetUrl,
        method: request.method,
    });

    return NextResponse.json(axiosResponse.data);
}

async function handleProxy(request: NextRequest) {
    const { pathname, search } = new URL(request.url);

    const proxyPrefix = "/api/proxy/";
    const endpointPath = pathname.startsWith(proxyPrefix) ? pathname.slice(proxyPrefix.length) : "";
    const targetUrl = `${API_URL}/${endpointPath}${search}`;
    const body = await request.json();

    const axiosResponse = await axios.request({
        url: targetUrl,
        method: request.method,
        data: body
    });

    return NextResponse.json(axiosResponse.data);
}
