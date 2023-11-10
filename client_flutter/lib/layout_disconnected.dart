import 'package:flutter/cupertino.dart';
import 'package:provider/provider.dart';
import 'app_data.dart';

class LayoutDisconnected extends StatefulWidget {
  const LayoutDisconnected({super.key});

  @override
  State<LayoutDisconnected> createState() => _LayoutDisconnectedState();
}

class _LayoutDisconnectedState extends State<LayoutDisconnected> {
  final _ipController = TextEditingController();
  final _portController = TextEditingController();

  Widget _buildTextFormField(
    String label,
    String defaultValue,
    TextEditingController controller,
  ) {
    controller.text = defaultValue;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.center,
      children: [
        Text(
          label,
          style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w200),
        ),
        Container(
          constraints: const BoxConstraints(maxWidth: 200),
          child: CupertinoTextField(controller: controller),
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    AppData appData = Provider.of<AppData>(context);

    _ipController.text = appData.ip;

    return CupertinoPageScaffold(
      navigationBar: const CupertinoNavigationBar(
        middle: Text("WebSockets Client"),
      ),
      child: ListView(
        padding: const EdgeInsets.all(20),
        children: [
          const SizedBox(height: 50),
          _buildTextFormField("Server IP", appData.ip, _ipController),
          const SizedBox(height: 20),
          _buildTextFormField("Server port", appData.port, _portController),
          const SizedBox(height: 20),
          Row(mainAxisAlignment: MainAxisAlignment.center, children: [
            SizedBox(
              width: 96,
              height: 32,
              child: CupertinoButton.filled(
                onPressed: () {
                  appData.ip = _ipController.text;
                  appData.port = _portController.text;
                  appData.connectToServer();
                },
                padding: EdgeInsets.zero,
                child: const Text(
                  "Connect",
                  style: TextStyle(
                    fontSize: 14,
                  ),
                ),
              ),
            ),
          ]),
          const SizedBox(height: 20),
        ],
      ),
    );
  }
}
