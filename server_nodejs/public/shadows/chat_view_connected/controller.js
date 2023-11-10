class ChatViewConnected extends HTMLElement {
    constructor() {
        super()
        this.shadow = this.attachShadow({ mode: 'open' })
    }

    async connectedCallback() {
        // Carrega els estils CSS
        const style = document.createElement('style')
        style.textContent = await fetch('/shadows/chat_view_connected/style.css').then(r => r.text())
        this.shadow.appendChild(style)
    
        // Carrega els elements HTML
        const htmlContent = await fetch('/shadows/chat_view_connected/view.html').then(r => r.text())

        // Converteix la cadena HTML en nodes utilitzant un DocumentFragment
        const template = document.createElement('template');
        template.innerHTML = htmlContent;
        
        // Clona i afegeix el contingut del template al shadow
        this.shadow.appendChild(template.content.cloneNode(true));

        // Definir els 'eventListeners' dels objectes 
        this.shadow.querySelector('#buttonDisconnect').addEventListener('click', this.actionDisconnect.bind(this))
        this.shadow.querySelector('#inputMessage').addEventListener('keyup', this.actionSendFromKeyDown.bind(this))
        this.shadow.querySelector('#buttonSend').addEventListener('click', this.actionSend.bind(this))
    } 

    async actionDisconnect () {
        disconnect()
        document.querySelector('chat-ws').showView('chat-view-disconnecting')
        await new Promise(resolve => setTimeout(resolve, 1500))
        document.querySelector('chat-ws').showView('chat-view-disconnected')
    }

    showInfo () {
        this.shadow.querySelector('#connectionInfo').innerHTML = `Connected to <b>${socket.url}</b>, with ID <b>${socketId}</b>`
    }

    addMessage (message) {
        let refMessages = this.shadow.querySelector('#messages')
        refMessages.innerHTML += message + "<br/>"
        refMessages.scrollTop = refMessages.scrollHeight;
    }

    setClients () {

        let refClients = this.shadow.querySelector('#clientsList')
        refClients.innerHTML = ""

        clients.forEach(client => {
            let obj = document.createElement('div')
            if (client === selectedClient) {
                obj.setAttribute('class', 'client clientSelected')
            } else {
                obj.setAttribute('class', 'client')
            }
            obj.innerHTML = client
            obj.addEventListener('click', this.actionClient.bind(this))
            refClients.appendChild(obj)
        })
    }

    actionClient (event) {
        let refClients = this.shadow.querySelector('#clientsList')
        let client = event.target.innerHTML
        if (client === selectedClient) {
            selectedClient = ""
            refClients.childNodes.forEach(element => {
                if (element.innerHTML === client) {
                    element.setAttribute('class', 'client')
                }
            })
        } else {
            selectedClient = client
            refClients.childNodes.forEach(element => {
                if (element.innerHTML === client) {
                    element.setAttribute('class', 'client clientSelected')
                } else {
                    element.setAttribute('class', 'client')
                }
            })
        }

        if (selectedClient != "") {
            this.shadow.querySelector('#buttonSend').innerHTML = "Private"
        } else {
            this.shadow.querySelector('#buttonSend').innerHTML = "Broadcast"
        }
    }

    actionSendFromKeyDown (event) {
        // Send if ENTER key is pressed
        if (event.keyCode === 13) {
            this.actionSend()
        }
    }

    actionSend() {
        let msg = this.shadow.querySelector('#inputMessage').value
        if (selectedClient == "") {
            broadcastMessage(msg)
        } else {
            privateMessage(msg)
        }
        this.shadow.querySelector('#inputMessage').value = ""
    }
}

// Defineix l'element personalitzat
customElements.define('chat-view-connected', ChatViewConnected)